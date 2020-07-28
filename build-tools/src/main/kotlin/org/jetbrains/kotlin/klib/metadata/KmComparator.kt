/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.klib.metadata

import kotlinx.metadata.*
import kotlinx.metadata.klib.*

/**
 * Structural comparison of Km* metadata.
 * [configuration] allows to tune comparison process.
 *
 */
internal class KmComparator(private val configuration: ComparisonConfig) {

    fun compare(kmClass1: KmClass, kmClass2: KmClass): MetadataCompareResult = serialComparator(
            compare(KmClass::name, ::compare) to "Different names: ${kmClass1.name}, ${kmClass2.name}",
            ::compareClassFlags to "Different flags for ${kmClass1.name}",
            compare(KmClass::constructors, compareLists(::compare)) to "Constructors mismatch for ${kmClass1.name}",
            compare(KmClass::properties, compareLists(::compare, KmProperty::mangle)) to "Properties mismatch for ${kmClass1.name}",
            compare(KmClass::functions, compareLists(::compare, KmFunction::mangle)) to "Functions mismatch for ${kmClass1.name}",
    )(kmClass1, kmClass2)

    fun compare(typealias1: KmTypeAlias, typealias2: KmTypeAlias): MetadataCompareResult = serialComparator(
            compare(KmTypeAlias::name, ::compare) to "Different names",
            compare(KmTypeAlias::underlyingType, ::compareTypes) to "Underlying types mismatch",
            compare(KmTypeAlias::expandedType, ::compareTypes) to "Expanded types mismatch",
            compare(KmTypeAlias::typeParameters, compareLists(::compare)) to "Type parameters mismatch"
    )(typealias1, typealias2)

    fun compare(function1: KmFunction, function2: KmFunction): MetadataCompareResult = serialComparator(
            compare(KmFunction::name, ::compare) to "Different names",
            compare(KmFunction::returnType, ::compareTypes) to "Return type mismatch",
            compare(KmFunction::valueParameters, compareLists(::compare)) to "Value parameters mismatch",
            ::compareFunctionFlags to "Flags mismatch"
    )(function1, function2)

    fun compare(property1: KmProperty, property2: KmProperty): MetadataCompareResult = serialComparator(
            compare(KmProperty::name, ::compare) to "Different names",
            compare(KmProperty::returnType, ::compareTypes) to "Return type mismatch",
            ::comparePropertyFlags to "Flags mismatch",
            (compare(KmProperty::getterFlags, ::comparePropertyAccessorFlags) to "Getter flags mismatch")
                    .takeIf { Flag.Property.HAS_GETTER(property1.flags) && Flag.Property.HAS_GETTER(property2.flags) },
            (compare(KmProperty::setterFlags, ::comparePropertyAccessorFlags) to "Setter flags mismatch")
                    .takeIf { Flag.Property.HAS_SETTER(property1.flags) && Flag.Property.HAS_SETTER(property2.flags) }
    )(property1, property2)

    private fun compare(entry1: KlibEnumEntry, entry2: KlibEnumEntry): MetadataCompareResult = serialComparator(
            compare(KlibEnumEntry::annotations, compareLists(::compare)) to "Different annotations",
            compare(KlibEnumEntry::name, ::compare) to "Different names",
            compare(KlibEnumEntry::ordinal, compareNullable(::compare)) to "Different ordinals"
    )(entry1, entry2)

    private fun checkFlag(flag: Flag, flagName: String? = null): (Flags, Flags) -> MetadataCompareResult = { f1, f2 ->
        when {
            flag(f1) != flag(f2) -> Fail("Flags mismatch: ${flag(f1)}, ${flag(f2)} for $flagName")
            else -> Ok
        }
    }

    private fun compare(value1: Int, value2: Int): MetadataCompareResult = when {
        value1 == value2 -> Ok
        else -> Fail("$value1 != $value2")
    }

    private fun compare(string1: String, string2: String): MetadataCompareResult = when {
        string1 == string2 -> Ok
        else -> Fail("$string1 != $string2")
    }

    private fun compareFunctionFlags(function1: KmFunction, function2: KmFunction): MetadataCompareResult =
            serialComparator(
                    checkFlag(Flag.Function.IS_EXTERNAL, "IS_EXTERNAL"),
                    checkFlag(Flag.Function.IS_DECLARATION, "IS_DECLARATION"),
                    ::compareVisibilityFlags,
                    ::compareModalityFlags
            )(function1.flags, function2.flags)

    private fun comparePropertyFlags(property1: KmProperty, property2: KmProperty): MetadataCompareResult =
            serialComparator(
                    checkFlag(Flag.Property.IS_CONST, "IS_CONST"),
                    checkFlag(Flag.Property.HAS_SETTER, "HAS_SETTER"),
                    checkFlag(Flag.Property.HAS_GETTER, "HAS_GETTER"),
                    checkFlag(Flag.Property.IS_VAR, "IS_VAR"),
                    checkFlag(Flag.Property.HAS_CONSTANT, "HAS_CONSTANT"),
                    checkFlag(Flag.Property.IS_DECLARATION, "IS_DECLARATION"),
                    checkFlag(Flag.Property.IS_EXTERNAL, "IS_EXTERNAL")
            )(property1.flags, property2.flags)

    private fun comparePropertyAccessorFlags(flags1: Flags, flags2: Flags): MetadataCompareResult = serialComparator(
            checkFlag(Flag.PropertyAccessor.IS_NOT_DEFAULT, "IS_NOT_DEFAULT"),
            checkFlag(Flag.PropertyAccessor.IS_INLINE, "IS_INLINE"),
            checkFlag(Flag.PropertyAccessor.IS_EXTERNAL, "IS_EXTERNAL"),
            ::compareVisibilityFlags,
            ::compareModalityFlags
    )(flags1, flags2)


    private fun compareClassFlags(class1: KmClass, class2: KmClass): MetadataCompareResult = serialComparator(
            checkFlag(Flag.Class.IS_CLASS, "IS_CLASS"),
            checkFlag(Flag.Class.IS_COMPANION_OBJECT, "IS_COMPANION_OBJECT"),
            checkFlag(Flag.Class.IS_ENUM_CLASS, "IS_ENUM_CLASS"),
            checkFlag(Flag.Class.IS_ENUM_ENTRY, "IS_ENUM_ENTRY"),
            checkFlag(Flag.Class.IS_OBJECT, "IS_OBJECT"),
            checkFlag(Flag.IS_FINAL, "IS_FINAL"),
            checkFlag(Flag.IS_OPEN, "IS_OPEN"),
            checkFlag(Flag.HAS_ANNOTATIONS, "HAS_ANNOTATIONS"),
            ::compareVisibilityFlags,
            ::compareModalityFlags
    )(class1.flags, class2.flags)

    private fun compareVisibilityFlags(flags1: Flags, flags2: Flags): MetadataCompareResult = serialComparator(
            checkFlag(Flag.IS_PUBLIC, "IS_PUBLIC"),
            checkFlag(Flag.IS_PRIVATE_TO_THIS, "IS_PRIVATE_TO_THIS"),
            checkFlag(Flag.IS_PRIVATE, "IS_PRIVATE"),
            checkFlag(Flag.IS_PROTECTED, "IS_PROTECTED"),
            checkFlag(Flag.IS_INTERNAL, "IS_INTERNAL")
    )(flags1, flags2)

    private fun compareModalityFlags(flags1: Flags, flags2: Flags): MetadataCompareResult = serialComparator(
            checkFlag(Flag.IS_FINAL, "IS_FINAL"),
            checkFlag(Flag.IS_ABSTRACT, "IS_ABSTRACT"),
            checkFlag(Flag.IS_OPEN, "IS_OPEN"),
            checkFlag(Flag.IS_SEALED, "IS_SEALED")
    )(flags1, flags2)

    private fun compare(annotation1: KmAnnotation, annotation2: KmAnnotation): MetadataCompareResult = when {
                annotation1.className != annotation2.className -> Fail("${annotation1.className} != ${annotation2.className}")
                // TODO: compare values
                else -> Ok
            }

    private fun compare(p1: KmValueParameter, p2: KmValueParameter): MetadataCompareResult = serialComparator(
            compare(KmValueParameter::name, ::compare) to "Different names",
            compare(KmValueParameter::type, compareNullable(::compareTypes)) to "Type mismatch",
            compare(KmValueParameter::annotations, compareLists(::compare)) to "Annotations mismatch"
    )(p1, p2)

    private fun compareTypeFlags(flags1: Flags, flags2: Flags): MetadataCompareResult = serialComparator(
            checkFlag(Flag.Type.IS_NULLABLE) to "Nullable flag mismatch",
            checkFlag(Flag.Type.IS_SUSPEND) to "Suspend flag mismatch"
    )(flags1, flags2)

    private fun compareConstructorFlags(flags1: Flags, flags2: Flags): MetadataCompareResult = serialComparator(
            checkFlag(Flag.Constructor.IS_PRIMARY) to "IS_PRIMARY mismatch"
    )(flags1, flags2)

    private fun compare(constructor1: KmConstructor, constructor2: KmConstructor): MetadataCompareResult = serialComparator(
            ::compareVisibilityFlags,
            ::compareConstructorFlags
    )(constructor1.flags, constructor2.flags)

    private fun compare(typeArgument1: KmTypeProjection, typeArgument2: KmTypeProjection): MetadataCompareResult = serialComparator(
            compare(KmTypeProjection::type, compareNullable(::compareTypes))
    )(typeArgument1, typeArgument2)

    private fun compare(typeParameter1: KmTypeParameter, typeParameter2: KmTypeParameter): MetadataCompareResult =
            when {
                typeParameter1.variance != typeParameter2.variance -> Fail("Different variance")
                typeParameter1.name != typeParameter2.name -> Fail("${typeParameter1.name}, ${typeParameter2.name}")
                else -> Ok
            }

    private fun compareTypes(type1: KmType, type2: KmType): MetadataCompareResult = serialComparator(
            compare(KmType::classifier, ::compare) to "Classifiers mismatch",
            compare(KmType::arguments, compareLists(::compare)) to "Type arguments mismatch",
            compare(KmType::flags, ::compareTypeFlags) to "Type flags mismatch for",
            compare(KmType::abbreviatedType, compareNullable(::compareTypes)) to "Abbreviated types mismatch"
    )(type1, type2)

    private fun compare(class1: KmClassifier, class2: KmClassifier): MetadataCompareResult = when {
        class1 is KmClassifier.TypeAlias && class2 is KmClassifier.TypeAlias -> {
            if (class1.name == class2.name) Ok else Fail("Different type aliases: ${class1.name}, ${class2.name}")
        }
        class1 is KmClassifier.Class && class2 is KmClassifier.Class -> {
            when (class1.name) {
                class2.name -> Ok
                else -> Fail("Different classes: ${class1.name}, ${class2.name}")
            }
        }
        class1 is KmClassifier.TypeParameter && class2 is KmClassifier.TypeParameter -> {
            // TODO: How to correctly compare type ids?
            Ok
        }
        else -> Fail("class1 is $class1 and class2 is $class2")
    }

    private fun <T, R> compare(
            property: T.() -> R,
            comparator: (R, R) -> MetadataCompareResult
    ): (T, T) -> MetadataCompareResult = { o1, o2 ->
        if (configuration.shouldCheck(property)) comparator(o1.property(), o2.property()) else Ok
    }
}