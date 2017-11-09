/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.android

import com.android.SdkConstants.CLASS_PARCEL
import com.android.SdkConstants.CLASS_PARCELABLE
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import kotlinx.android.parcel.Parcelize
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.intentions.getLeftMostReceiverExpression
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.propertyDescriptor
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType


private val CREATOR_NAME = "CREATOR"
private val PARCEL_NAME = "parcel"
private val CREATOR_TEXT =
        "companion object $CREATOR_NAME : android.os.Parcelable.Creator<%1\$s> {\n" +
        "    override fun createFromParcel($PARCEL_NAME: $CLASS_PARCEL): %1\$s {\n" +
        "        return %1\$s($PARCEL_NAME)\n" +
        "    }\n\n" +
        "    override fun newArray(size: Int): Array<%1\$s?> {\n" +
        "        return arrayOfNulls(size)\n" +
        "    }\n" +
        "}"
private val WRITE_TO_PARCEL_TEXT = "override fun writeToParcel($PARCEL_NAME: $CLASS_PARCEL, flags: Int) {\n}"
private val WRITE_TO_PARCEL_SUPER_CALL_TEXT = "super.writeToParcel($PARCEL_NAME, flags)"
private val WRITE_TO_PARCEL_WITH_SUPER_TEXT =
        "override fun writeToParcel($PARCEL_NAME: $CLASS_PARCEL, flags: Int) {\n$WRITE_TO_PARCEL_SUPER_CALL_TEXT\n}"
private val DESCRIBE_CONTENTS_TEXT = "override fun describeContents(): Int {\nreturn 0\n}"
private val CONSTRUCTOR_TEXT = "constructor($PARCEL_NAME: $CLASS_PARCEL)"

private val PARCELIZE_FQNAME = FqName(Parcelize::class.java.name)

//TODO add test
fun KtClass.isParcelize() = findAnnotation(PARCELIZE_FQNAME) != null

fun KtClass.canAddParcelable(): Boolean =
        findParcelableSupertype() == null
        || findCreator() == null
        || findConstructorFromParcel() == null
        || findWriteToParcel() == null
        || findDescribeContents() == null

fun KtClass.canRedoParcelable(): Boolean = canRemoveParcelable()

fun KtClass.canRemoveParcelable(): Boolean =
        findParcelableSupertype()?.takeIf { it.typeReference?.isParcelableReference() ?: false }
        ?: findCreator()
        ?: findConstructorFromParcel()
        ?: findWriteToParcel()
        ?: findDescribeContents() != null

fun KtClass.implementParcelable() {
    val factory = KtPsiFactory(this)
    val superExtendsParcelable = superExtendsParcelable()

    findOrCreateParcelableSupertype(factory)
    val constructor = findOrCreateConstructor(factory)

    addFieldReads(constructor, factory)

    val writeToParcel = findOrCreateWriteToParcel(factory, superExtendsParcelable)
    addFieldWrites(writeToParcel, factory, superExtendsParcelable)

    findOrCreateDescribeContents(factory)
    findOrCreateCreator(factory)

    performDelayedRefactoringRequests(project)
    save()
}

fun KtClass.removeParcelableImplementation() {
    findParcelableSupertype()?.takeIf { it.typeReference?.isParcelableReference() ?: false }?.let {
        removeSuperTypeListEntry(it)
    }

    findConstructorFromParcel()?.let {
        if (it is KtPrimaryConstructor) {
            tryDeleteInitBlock()
        }
        it.delete()
    }

    findWriteToParcel()?.delete()
    findDescribeContents()?.delete()
    findCreator()?.delete()
    save()
}

fun KtClass.reimplementParcelable() {
    removeParcelableImplementation()
    implementParcelable()
}

private fun KtClass.findCreator(): KtObjectDeclaration? = companionObjects.find { it.name == CREATOR_NAME }

private fun KtClass.findParcelableSupertype(): KtSuperTypeListEntry? = getSuperTypeList()?.findParcelable()

private fun KtSuperTypeList.findParcelable() = entries?.find { it.typeReference?.isParcelableSuccessorReference() ?: false }

private fun KtTypeReference.isParcelableSuccessorReference() =
        analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, this]?.isSubclassOfParcelable() ?: false

private fun KtClass.superExtendsParcelable() = superTypeListEntries.find { it.typeReference?.extendsParcelable() ?: false } != null

private fun KtClass.tryDeleteInitBlock() {
    getAnonymousInitializers().forEach { initializer ->
        val block = initializer.body as? KtBlockExpression
        if (block != null) {
            block.statements.forEach { statement ->
                if (statement.isReadFromParcelPropertyAssignment()) {
                    statement.delete()
                }
            }

            if (block.statements.isEmpty()) {
                initializer.delete()
            }
        }
    }
}

private fun KtExpression.isReadFromParcelPropertyAssignment(): Boolean {
    if (this !is KtBinaryExpression || this.operationToken != KtTokens.EQ) {
        return false
    }

    return this.right?.isReadFromParcel() ?: false
}

private fun KtExpression.isReadFromParcel(): Boolean {
    val reference = firstChild as? KtReferenceExpression
                    ?: (firstChild as? KtDotQualifiedExpression)?.getLeftMostReceiverExpression() as? KtReferenceExpression ?: return false
    val target = reference.analyze(BodyResolveMode.PARTIAL)[BindingContext.REFERENCE_TARGET, reference] ?: return false
    return (target as? ParameterDescriptor)?.type?.fqNameEquals(CLASS_PARCEL) ?: false
}

private fun KtClass.addFieldWrites(function: KtFunction, factory: KtPsiFactory, callSuper: Boolean) {
    val bodyExpression = function.bodyExpression
    if (bodyExpression !is KtBlockExpression || !bodyExpression.isEmptyWriteToParcel(callSuper)) {
        return
    }

    val propertyParameterDescriptors = primaryConstructor?.valueParameters?.mapNotNull { it.propertyDescriptor } ?: emptyList()

    val propertyDescriptors = declarations
            .filter { it.isParcelableProperty() }
            .mapNotNull { it.descriptor as? PropertyDescriptor }

    val parcelName = function.valueParameters[0].name ?: return
    val flagsName = function.valueParameters[1].name ?: return
    val blockText =
            (propertyParameterDescriptors + propertyDescriptors)
            .mapNotNull { it.formatWriteToParcel(parcelName, flagsName) }
            .joinToString(separator = "\n")

    val block = factory.createBlock(
            if (callSuper)
                WRITE_TO_PARCEL_SUPER_CALL_TEXT + if (blockText.isNotBlank()) "\n$blockText" else ""
            else blockText
    )

    bodyExpression.replace(block)
}

private fun KtClass.addFieldReads(constructor: KtConstructor<*>, factory: KtPsiFactory) {
    val bodyExpression = constructor.getBodyExpression()
    if (bodyExpression != null && bodyExpression.statements.isNotEmpty()) {
        return
    }

    val parcelName = constructor.getValueParameters().firstOrNull()?.name ?: return
    val parcelableProperties = declarations
            .filter { it.isParcelableProperty() }
            .mapNotNull { it.descriptor as? PropertyDescriptor }

    if (parcelableProperties.isEmpty()) {
        return
    }

    val blockText = parcelableProperties
            .mapNotNull { descriptor -> descriptor.formatReadFromParcel(parcelName)?.let { "${descriptor.name} = $it" } }
            .joinToString(separator = "\n")

    val block = factory.createBlock(blockText)

    if (constructor is KtPrimaryConstructor) {
        val initializer = factory.createAnonymousInitializer()
        initializer.body?.replace(block)
        addDeclaration(initializer).apply {
            addNewLineBeforeDeclaration()
            addToShorteningWaitSet()
        }
    }
    else {
        bodyExpression?.replace(block) ?: constructor.add(block)
    }
}

private fun  KtDeclaration.isParcelableProperty(): Boolean =
        this is KtProperty && isVar && !hasDelegate() && !isTransient() && getter == null && setter == null

private fun KtProperty.isTransient() = annotationEntries.find { it.isTransientAnnotation() } != null

private fun KtAnnotationEntry.isTransientAnnotation(): Boolean =
    typeReference?.analyze(BodyResolveMode.PARTIAL)?.get(BindingContext.TYPE, typeReference)?.fqNameEquals("kotlin.jvm.Transient") ?: false

private fun KtExpression.isCallToSuperWriteToParcel() =
        this is KtDotQualifiedExpression
        && receiverExpression is KtSuperExpression
        && (selectorExpression as? KtCallExpression)?.calleeExpression?.text == "writeToParcel"

private fun KtBlockExpression.isEmptyWriteToParcel(callSuper: Boolean): Boolean =
        if (callSuper) {
            statements.isEmpty() || statements.size == 1 && statements.first().isCallToSuperWriteToParcel()
        }
        else {
            statements.isEmpty()
        }

private fun PropertyDescriptor.formatReadFromParcel(parcelName: String): String? {
    val type = returnType ?: return null

    fun KotlinType.formatJavaClassloader() = "${this.constructor.declarationDescriptor?.fqNameSafe}::class.java.classLoader"

    // There is no read/write short array methods
    if (KotlinBuiltIns.isPrimitiveArray(type) && KotlinBuiltIns.getPrimitiveArrayElementType(type) != PrimitiveType.SHORT) {
        return "$parcelName.create${type.getName()}()"
    }


    if (type.isMarkedNullable && KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(type)) {
        return "$parcelName.readValue(${type.formatJavaClassloader()}) as? ${type.getName()}"
    }

    if (KotlinBuiltIns.isPrimitiveType(type)) {
        return when {
            KotlinBuiltIns.isBoolean(type) -> "$parcelName.readByte() != 0.toByte()"
            KotlinBuiltIns.isShort(type) -> "$parcelName.readInt().toShort()"
            KotlinBuiltIns.isChar(type) -> "$parcelName.readInt().toChar()"
            else -> "$parcelName.read${type.getName()}()"
        }
    }

    return when {
        KotlinBuiltIns.isCharSequenceOrNullableCharSequence(type) -> "$parcelName.readString()"
        KotlinBuiltIns.isStringOrNullableString(type) -> "$parcelName.readString()"
        type.isArrayOfParcelable() -> "$parcelName.createTypedArray(${type.arguments.single().type.getName()}.CREATOR)"
        type.isListOfParcelable() -> "$parcelName.createTypedArrayList(${type.arguments.single().type.getName()}.CREATOR)"
        type.isArrayOfIBinder() -> "$parcelName.createBinderArray()"
        type.isListOfIBinder() -> "$parcelName.createBinderArrayList()"
        type.isArrayOfString() -> "$parcelName.createStringArray()"
        type.isListOfString() -> "$parcelName.createStringArrayList()"
        type.isSparseBooleanArray() -> "$parcelName.readSparseBooleanArray()"
        type.isBundle() -> "$parcelName.readBundle(${type.formatJavaClassloader()})"
        type.isIBinder() -> "$parcelName.readStrongBinder()"
        type.isSubclassOfParcelable(true) -> "$parcelName.readParcelable(${type.formatJavaClassloader()})"  // This one should go last
        else -> null
    }
}

private fun PropertyDescriptor.formatWriteToParcel(parcelName: String, flagsName: String): String? {
    val type = returnType ?: return null

    // There is no read/write short array methods
    if (KotlinBuiltIns.isPrimitiveArray(type) && KotlinBuiltIns.getPrimitiveArrayElementType(type) != PrimitiveType.SHORT) {
        return "$parcelName.write${type.getName()}($name)"
    }

    if (type.isMarkedNullable) {
        if (KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(type)) {
            return "$parcelName.writeValue($name)"
        }
        else if (KotlinBuiltIns.isCharSequenceOrNullableCharSequence(type)) {
            return "$parcelName.writeString($name?.toString())"
        }
    }

    if (KotlinBuiltIns.isPrimitiveType(type)) {
        return when {
            KotlinBuiltIns.isBoolean(type) -> "$parcelName.writeByte(if ($name)  1 else 0)"
            KotlinBuiltIns.isShort(type) -> "$parcelName.writeInt($name.toInt())"
            KotlinBuiltIns.isChar(type) -> "$parcelName.writeInt($name.toInt())"
            else -> "$parcelName.write${type.getName()}($name)"
        }
    }

    return when {
        KotlinBuiltIns.isCharSequence(type) -> "$parcelName.writeString($name.toString())"
        KotlinBuiltIns.isStringOrNullableString(type) -> "$parcelName.writeString($name)"
        type.isArrayOfParcelable() -> "$parcelName.writeTypedArray($name, $flagsName)"
        type.isListOfParcelable() -> "$parcelName.writeTypedList($name)"
        type.isArrayOfIBinder() -> "$parcelName.writeBinderArray($name)"
        type.isListOfIBinder() -> "$parcelName.writeBinderList($name)"
        type.isArrayOfString() -> "$parcelName.writeStringArray($name)"
        type.isListOfString() -> "$parcelName.writeStringList($name)"
        type.isSparseBooleanArray() -> "$parcelName.writeSparseBooleanArray($name)"
        type.isBundle() -> "$parcelName.writeBundle($name)"
        type.isIBinder() -> "$parcelName.writeStrongBinder($name)"
        type.isSubclassOfParcelable(true) -> "$parcelName.writeParcelable($name, $flagsName)" // This one should go last
        else -> null
    }
}

private fun KtClass.findOrCreateCreator(factory: KtPsiFactory): KtClassOrObject {
    findCreator()?.let {
        return it
    }

    val creator = factory.createObject(CREATOR_TEXT.format(name))
    return addDeclaration(creator).apply { addToShorteningWaitSet() }
}

private fun KtClass.findOrCreateParcelableSupertype(factory: KtPsiFactory): KtSuperTypeListEntry? {
    findParcelableSupertype()?.let {
        return it
    }

    val supertypeEntry = factory.createSuperTypeEntry(CLASS_PARCELABLE)
    return addSuperTypeListEntry(supertypeEntry).apply { addToShorteningWaitSet() }
}

private fun KtClass.save() = FileDocumentManager.getInstance().getDocument(containingFile.virtualFile)?.let {
    PsiDocumentManager.getInstance(project).commitDocument(it)
}

private fun KtClass.findOrCreateConstructor(factory: KtPsiFactory): KtConstructor<*> {
    findConstructorFromParcel()?.let {
        return it
    }

    createPrimaryConstructorIfAbsent()
    return createSecondaryConstructor(factory).apply {
        addToShorteningWaitSet()
    }
}

private fun KtClass.createSecondaryConstructor(factory: KtPsiFactory): KtConstructor<*> {
    val constructorText = primaryConstructor?.let { constructor ->
        val arguments = constructor.valueParameters.map {
            it?.propertyDescriptor?.formatReadFromParcel(PARCEL_NAME) ?: "TODO(\"${it?.name}\")"
        }

        val argumentList = arguments.joinToString(
                prefix = if (arguments.size > 1) "(\n" else "(",
                postfix = ")",
                separator = if (arguments.size > 1) ",\n" else ", ")

        "$CONSTRUCTOR_TEXT :this$argumentList {\n}"
    } ?: "$CONSTRUCTOR_TEXT {\n}"

    val constructor =  factory.createSecondaryConstructor(constructorText)
    val lastProperty = declarations.findLast { it is KtProperty }
    return if (lastProperty != null) {
        addDeclarationAfter(constructor, lastProperty).apply { addNewLineBeforeDeclaration() }
    }
    else {
        val firstFunction = declarations.find { it is KtFunction }
        addDeclarationBefore(constructor, firstFunction).apply { addNewLineBeforeDeclaration() }
    }
}

private fun KtTypeReference.extendsParcelable(): Boolean =
        analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, this]?.isSubclassOfParcelable(true) ?: false

private fun KtClass.findWriteToParcel() = declarations.find { it.isWriteToParcel() }

private fun KtDeclaration.isWriteToParcel(): Boolean = this is KtFunction && name == "writeToParcel" && valueParameters.let {
    it.size == 2 && it[0].isParcelParameter() && (it[1].typeReference?.fqNameEquals("kotlin.Int") ?: false)
}

private fun KtClass.findOrCreateWriteToParcel(factory: KtPsiFactory, callSuper: Boolean): KtFunction {
    findWriteToParcel()?.let {
        return it as KtFunction
    }

    val writeToParcel = factory.createFunction(if (callSuper) WRITE_TO_PARCEL_WITH_SUPER_TEXT else WRITE_TO_PARCEL_TEXT)
    return addDeclaration(writeToParcel).apply {
        addNewLineBeforeDeclaration()
        addToShorteningWaitSet()
    }
}

private fun KtClass.findDescribeContents() = declarations.find { it.isDescribeContents() }

private fun KtDeclaration.isDescribeContents(): Boolean = this is KtFunction &&
                                                          name == "describeContents" &&
                                                          valueParameters.isEmpty()

private fun KtClass.findOrCreateDescribeContents(factory: KtPsiFactory): KtFunction {
    findDescribeContents()?.let {
        return it as KtFunction
    }

    val describeContents = factory.createFunction(DESCRIBE_CONTENTS_TEXT)
    return addDeclaration(describeContents).apply {
        addNewLineBeforeDeclaration()
        addToShorteningWaitSet()
    }
}

private fun KtClass.findConstructorFromParcel(): KtConstructor<*>? =
    primaryConstructor?.takeIf { it.isConstructorFromParcel() } ?: secondaryConstructors.find { it.isConstructorFromParcel() }

private fun KtConstructor<*>.isConstructorFromParcel(): Boolean = getValueParameters().let {
    it.size == 1 && it.single().isParcelParameter()
}

private fun KtParameter.isParcelParameter(): Boolean = typeReference?.fqNameEquals(CLASS_PARCEL) ?: false

private fun KtTypeReference.isParcelableReference() = fqNameEquals(CLASS_PARCELABLE)

private fun KtTypeReference.fqNameEquals(fqName: String) =
        analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, this]?.fqNameEquals(fqName) ?: false

private fun KotlinType.getName() = constructor.declarationDescriptor?.name

private fun KotlinType.fqNameEquals(fqName: String) = constructor.declarationDescriptor?.fqNameSafe?.asString() == fqName

private fun KotlinType.isSubclassOfParcelable(strict: Boolean = false): Boolean = isSubclassOf(CLASS_PARCELABLE, strict)

private fun KotlinType.isIBinder(): Boolean = fqNameEquals("android.os.IBinder")

private fun KotlinType.isArrayOfParcelable(): Boolean =
        KotlinBuiltIns.isArray(this) && arguments.singleOrNull()?.type?.isSubclassOfParcelable(true) ?: false

private fun KotlinType.isArrayOfIBinder(): Boolean =
        KotlinBuiltIns.isArray(this) && arguments.singleOrNull()?.type?.isIBinder() ?: false

private fun KotlinType.isArrayOfString(): Boolean =
        KotlinBuiltIns.isArray(this) && KotlinBuiltIns.isStringOrNullableString(arguments.singleOrNull()?.type)

private fun KotlinType.isListOfString(): Boolean =
        KotlinBuiltIns.isListOrNullableList(this) && KotlinBuiltIns.isStringOrNullableString(arguments.singleOrNull()?.type)

private fun KotlinType.isListOfParcelable(): Boolean =
        KotlinBuiltIns.isListOrNullableList(this) && arguments.singleOrNull()?.type?.isSubclassOfParcelable(true) ?: false

private fun KotlinType.isListOfIBinder(): Boolean =
        KotlinBuiltIns.isListOrNullableList(this) && arguments.singleOrNull()?.type?.isIBinder() ?: false

private fun KotlinType.isSparseBooleanArray(): Boolean = fqNameEquals("android.util.SparseBooleanArray")

private fun KotlinType.isBundle(): Boolean = fqNameEquals("android.os.Bundle")

private fun <T: KtDeclaration> T.addNewLineBeforeDeclaration() = parent.addBefore(KtPsiFactory(this).createNewLine(), this)