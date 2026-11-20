// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_STDLIB
// ISSUE: KT-57456, KT-57608, KT-74926
@file:OptIn(ExperimentalContracts::class)

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

enum class Enum {
    A {
        val memberAInside = <!UNINITIALIZED_ENUM_COMPANION!>value<!>
        val memberBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
        val memberCInside = nonInPlaceRun { value }
        val memberDInside by <!UNINITIALIZED_ENUM_COMPANION!>value<!>
        val memberEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
        val memberFInside by nonInPlaceDelegate { value }

        val companionNameReferenceAInside = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
        val companionNameReferenceBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
        val companionNameReferenceCInside = nonInPlaceRun { Companion }
        val companionNameReferenceDInside by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
        val companionNameReferenceEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
        val companionNameReferenceFInside by nonInPlaceDelegate { Companion }

        val classNameReferenceAInside = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>
        val classNameReferenceBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!> }
        val classNameReferenceCInside = nonInPlaceRun { Enum }
        val classNameReferenceDInside by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>
        val classNameReferenceEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!> }
        val classNameReferenceFInside by nonInPlaceDelegate { Enum }

        val classAndCompanionNameReferenceAInside = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
        val classAndCompanionNameReferenceBInside = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
        val classAndCompanionNameReferenceCInside = nonInPlaceRun { Enum.Companion }
        val classAndCompanionNameReferenceDInside by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
        val classAndCompanionNameReferenceEInside by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
        val classAndCompanionNameReferenceFInside by nonInPlaceDelegate { Enum.Companion }

        val companionAliasReferenceAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>
        val companionAliasReferenceBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!> }
        val companionAliasReferenceCInside = nonInPlaceRun { EnumCompanionAlias }
        val companionAliasReferenceDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>
        val companionAliasReferenceEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!> }
        val companionAliasReferenceFInside by nonInPlaceDelegate { EnumCompanionAlias }

        val nestedCompanionAliasReferenceAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>
        val nestedCompanionAliasReferenceBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!> }
        val nestedCompanionAliasReferenceCInside = nonInPlaceRun { NestedEnumCompanionAlias }
        val nestedCompanionAliasReferenceDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>
        val nestedCompanionAliasReferenceEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!> }
        val nestedCompanionAliasReferenceFInside by nonInPlaceDelegate { NestedEnumCompanionAlias }

        val classAliasReferenceAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>
        val classAliasReferenceBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!> }
        val classAliasReferenceCInside = nonInPlaceRun { EnumAlias }
        val classAliasReferenceDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>
        val classAliasReferenceEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!> }
        val classAliasReferenceFInside by nonInPlaceDelegate { EnumAlias }

        val nestedClassAliasReferenceAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>
        val nestedClassAliasReferenceBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!> }
        val nestedClassAliasReferenceCInside = nonInPlaceRun { NestedEnumAlias }
        val nestedClassAliasReferenceDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>
        val nestedClassAliasReferenceEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!> }
        val nestedClassAliasReferenceFInside by nonInPlaceDelegate { NestedEnumAlias }

        val companionNameAInside = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
        val companionNameBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
        val companionNameCInside = nonInPlaceRun { Companion.value }
        val companionNameDInside by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
        val companionNameEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
        val companionNameFInside by nonInPlaceDelegate { Companion.value }

        val classNameAInside = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value
        val classNameBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value }
        val classNameCInside = nonInPlaceRun { Enum.value }
        val classNameDInside by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value
        val classNameEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value }
        val classNameFInside by nonInPlaceDelegate { Enum.value }

        val classAndCompanionNameAInside = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
        val classAndCompanionNameBInside = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
        val classAndCompanionNameCInside = nonInPlaceRun { Enum.Companion.value }
        val classAndCompanionNameDInside by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
        val classAndCompanionNameEInside by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
        val classAndCompanionNameFInside by nonInPlaceDelegate { Enum.Companion.value }

        val companionAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value
        val companionAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value }
        val companionAliasCInside = nonInPlaceRun { EnumCompanionAlias.value }
        val companionAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value
        val companionAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value }
        val companionAliasFInside by nonInPlaceDelegate { EnumCompanionAlias.value }

        val nestedCompanionAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value
        val nestedCompanionAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value }
        val nestedCompanionAliasCInside = nonInPlaceRun { NestedEnumCompanionAlias.value }
        val nestedCompanionAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value
        val nestedCompanionAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value }
        val nestedCompanionAliasFInside by nonInPlaceDelegate { NestedEnumCompanionAlias.value }

        val classAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value
        val classAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value }
        val classAliasCInside = nonInPlaceRun { EnumAlias.value }
        val classAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value
        val classAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value }
        val classAliasFInside by nonInPlaceDelegate { EnumAlias.value }

        val nestedClassAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value
        val nestedClassAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value }
        val nestedClassAliasCInside = nonInPlaceRun { NestedEnumAlias.value }
        val nestedClassAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value
        val nestedClassAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value }
        val nestedClassAliasFInside by nonInPlaceDelegate { NestedEnumAlias.value }
    },
    B {
        init {
            val memberAInit = <!UNINITIALIZED_ENUM_COMPANION!>value<!>
            val memberBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
            val memberCInit = nonInPlaceRun { value }
            val memberDInit by <!UNINITIALIZED_ENUM_COMPANION!>value<!>
            val memberEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
            val memberFInit by nonInPlaceDelegate { value }

            val companionNameReferenceAInit = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
            val companionNameReferenceBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
            val companionNameReferenceCInit = nonInPlaceRun { Companion }
            val companionNameReferenceDInit by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
            val companionNameReferenceEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
            val companionNameReferenceFInit by nonInPlaceDelegate { Companion }

            val classNameReferenceAInit = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>
            val classNameReferenceBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!> }
            val classNameReferenceCInit = nonInPlaceRun { Enum }
            val classNameReferenceDInit by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>
            val classNameReferenceEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!> }
            val classNameReferenceFInit by nonInPlaceDelegate { Enum }

            val classAndCompanionNameReferenceAInit = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
            val classAndCompanionNameReferenceBInit = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
            val classAndCompanionNameReferenceCInit = nonInPlaceRun { Enum.Companion }
            val classAndCompanionNameReferenceDInit by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
            val classAndCompanionNameReferenceEInit by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
            val classAndCompanionNameReferenceFInit by nonInPlaceDelegate { Enum.Companion }

            val companionAliasReferenceAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>
            val companionAliasReferenceBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!> }
            val companionAliasReferenceCInit = nonInPlaceRun { EnumCompanionAlias }
            val companionAliasReferenceDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>
            val companionAliasReferenceEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!> }
            val companionAliasReferenceFInit by nonInPlaceDelegate { EnumCompanionAlias }

            val nestedCompanionAliasReferenceAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>
            val nestedCompanionAliasReferenceBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!> }
            val nestedCompanionAliasReferenceCInit = nonInPlaceRun { NestedEnumCompanionAlias }
            val nestedCompanionAliasReferenceDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>
            val nestedCompanionAliasReferenceEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!> }
            val nestedCompanionAliasReferenceFInit by nonInPlaceDelegate { NestedEnumCompanionAlias }

            val classAliasReferenceAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>
            val classAliasReferenceBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!> }
            val classAliasReferenceCInit = nonInPlaceRun { EnumAlias }
            val classAliasReferenceDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>
            val classAliasReferenceEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!> }
            val classAliasReferenceFInit by nonInPlaceDelegate { EnumAlias }

            val nestedClassAliasReferenceAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>
            val nestedClassAliasReferenceBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!> }
            val nestedClassAliasReferenceCInit = nonInPlaceRun { NestedEnumAlias }
            val nestedClassAliasReferenceDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>
            val nestedClassAliasReferenceEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!> }
            val nestedClassAliasReferenceFInit by nonInPlaceDelegate { NestedEnumAlias }

            val companionNameAInit = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
            val companionNameBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
            val companionNameCInit = nonInPlaceRun { Companion.value }
            val companionNameDInit by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
            val companionNameEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
            val companionNameFInit by nonInPlaceDelegate { Companion.value }

            val classNameAInit = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value
            val classNameBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value }
            val classNameCInit = nonInPlaceRun { Enum.value }
            val classNameDInit by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value
            val classNameEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value }
            val classNameFInit by nonInPlaceDelegate { Enum.value }

            val classAndCompanionNameAInit = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
            val classAndCompanionNameBInit = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
            val classAndCompanionNameCInit = nonInPlaceRun { Enum.Companion.value }
            val classAndCompanionNameDInit by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
            val classAndCompanionNameEInit by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
            val classAndCompanionNameFInit by nonInPlaceDelegate { Enum.Companion.value }

            val companionAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value
            val companionAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value }
            val companionAliasCInit = nonInPlaceRun { EnumCompanionAlias.value }
            val companionAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value
            val companionAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value }
            val companionAliasFInit by nonInPlaceDelegate { EnumCompanionAlias.value }

            val nestedCompanionAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value
            val nestedCompanionAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value }
            val nestedCompanionAliasCInit = nonInPlaceRun { NestedEnumCompanionAlias.value }
            val nestedCompanionAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value
            val nestedCompanionAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value }
            val nestedCompanionAliasFInit by nonInPlaceDelegate { NestedEnumCompanionAlias.value }

            val classAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value
            val classAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value }
            val classAliasCInit = nonInPlaceRun { EnumAlias.value }
            val classAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value
            val classAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value }
            val classAliasFInit by nonInPlaceDelegate { EnumAlias.value }

            val nestedClassAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value
            val nestedClassAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value }
            val nestedClassAliasCInit = nonInPlaceRun { NestedEnumAlias.value }
            val nestedClassAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value
            val nestedClassAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value }
            val nestedClassAliasFInit by nonInPlaceDelegate { NestedEnumAlias.value }
        }
    },
    C {
        init {
            class Local {
                val memberAInside = value
                val memberBInside = inPlaceRun { value }
                val memberCInside = nonInPlaceRun { value }
                val memberDInside by value
                val memberEInside by inPlaceDelegate { value }
                val memberFInside by nonInPlaceDelegate { value }

                val companionNameReferenceAInside = Companion
                val companionNameReferenceBInside = inPlaceRun { Companion }
                val companionNameReferenceCInside = nonInPlaceRun { Companion }
                val companionNameReferenceDInside by Companion
                val companionNameReferenceEInside by inPlaceDelegate { Companion }
                val companionNameReferenceFInside by nonInPlaceDelegate { Companion }

                val classNameReferenceAInside = Enum
                val classNameReferenceBInside = inPlaceRun { Enum }
                val classNameReferenceCInside = nonInPlaceRun { Enum }
                val classNameReferenceDInside by Enum
                val classNameReferenceEInside by inPlaceDelegate { Enum }
                val classNameReferenceFInside by nonInPlaceDelegate { Enum }

                val classAndCompanionNameReferenceAInside = Enum.Companion
                val classAndCompanionNameReferenceBInside = inPlaceRun { Enum.Companion }
                val classAndCompanionNameReferenceCInside = nonInPlaceRun { Enum.Companion }
                val classAndCompanionNameReferenceDInside by Enum.Companion
                val classAndCompanionNameReferenceEInside by inPlaceDelegate { Enum.Companion }
                val classAndCompanionNameReferenceFInside by nonInPlaceDelegate { Enum.Companion }

                val companionAliasReferenceAInside = EnumCompanionAlias
                val companionAliasReferenceBInside = inPlaceRun { EnumCompanionAlias }
                val companionAliasReferenceCInside = nonInPlaceRun { EnumCompanionAlias }
                val companionAliasReferenceDInside by EnumCompanionAlias
                val companionAliasReferenceEInside by inPlaceDelegate { EnumCompanionAlias }
                val companionAliasReferenceFInside by nonInPlaceDelegate { EnumCompanionAlias }

                val nestedCompanionAliasReferenceAInside = NestedEnumCompanionAlias
                val nestedCompanionAliasReferenceBInside = inPlaceRun { NestedEnumCompanionAlias }
                val nestedCompanionAliasReferenceCInside = nonInPlaceRun { NestedEnumCompanionAlias }
                val nestedCompanionAliasReferenceDInside by NestedEnumCompanionAlias
                val nestedCompanionAliasReferenceEInside by inPlaceDelegate { NestedEnumCompanionAlias }
                val nestedCompanionAliasReferenceFInside by nonInPlaceDelegate { NestedEnumCompanionAlias }

                val classAliasReferenceAInside = EnumAlias
                val classAliasReferenceBInside = inPlaceRun { EnumAlias }
                val classAliasReferenceCInside = nonInPlaceRun { EnumAlias }
                val classAliasReferenceDInside by EnumAlias
                val classAliasReferenceEInside by inPlaceDelegate { EnumAlias }
                val classAliasReferenceFInside by nonInPlaceDelegate { EnumAlias }

                val nestedClassAliasReferenceAInside = NestedEnumAlias
                val nestedClassAliasReferenceBInside = inPlaceRun { NestedEnumAlias }
                val nestedClassAliasReferenceCInside = nonInPlaceRun { NestedEnumAlias }
                val nestedClassAliasReferenceDInside by NestedEnumAlias
                val nestedClassAliasReferenceEInside by inPlaceDelegate { NestedEnumAlias }
                val nestedClassAliasReferenceFInside by nonInPlaceDelegate { NestedEnumAlias }

                val companionNameAInside = Companion.value
                val companionNameBInside = inPlaceRun { Companion.value }
                val companionNameCInside = nonInPlaceRun { Companion.value }
                val companionNameDInside by Companion.value
                val companionNameEInside by inPlaceDelegate { Companion.value }
                val companionNameFInside by nonInPlaceDelegate { Companion.value }

                val classNameAInside = Enum.value
                val classNameBInside = inPlaceRun { Enum.value }
                val classNameCInside = nonInPlaceRun { Enum.value }
                val classNameDInside by Enum.value
                val classNameEInside by inPlaceDelegate { Enum.value }
                val classNameFInside by nonInPlaceDelegate { Enum.value }

                val classAndCompanionNameAInside = Enum.Companion.value
                val classAndCompanionNameBInside = inPlaceRun { Enum.Companion.value }
                val classAndCompanionNameCInside = nonInPlaceRun { Enum.Companion.value }
                val classAndCompanionNameDInside by Enum.Companion.value
                val classAndCompanionNameEInside by inPlaceDelegate { Enum.Companion.value }
                val classAndCompanionNameFInside by nonInPlaceDelegate { Enum.Companion.value }

                val companionAliasAInside = EnumCompanionAlias.value
                val companionAliasBInside = inPlaceRun { EnumCompanionAlias.value }
                val companionAliasCInside = nonInPlaceRun { EnumCompanionAlias.value }
                val companionAliasDInside by EnumCompanionAlias.value
                val companionAliasEInside by inPlaceDelegate { EnumCompanionAlias.value }
                val companionAliasFInside by nonInPlaceDelegate { EnumCompanionAlias.value }

                val nestedCompanionAliasAInside = NestedEnumCompanionAlias.value
                val nestedCompanionAliasBInside = inPlaceRun { NestedEnumCompanionAlias.value }
                val nestedCompanionAliasCInside = nonInPlaceRun { NestedEnumCompanionAlias.value }
                val nestedCompanionAliasDInside by NestedEnumCompanionAlias.value
                val nestedCompanionAliasEInside by inPlaceDelegate { NestedEnumCompanionAlias.value }
                val nestedCompanionAliasFInside by nonInPlaceDelegate { NestedEnumCompanionAlias.value }

                val classAliasAInside = EnumAlias.value
                val classAliasBInside = inPlaceRun { EnumAlias.value }
                val classAliasCInside = nonInPlaceRun { EnumAlias.value }
                val classAliasDInside by EnumAlias.value
                val classAliasEInside by inPlaceDelegate { EnumAlias.value }
                val classAliasFInside by nonInPlaceDelegate { EnumAlias.value }

                val nestedClassAliasAInside = NestedEnumAlias.value
                val nestedClassAliasBInside = inPlaceRun { NestedEnumAlias.value }
                val nestedClassAliasCInside = nonInPlaceRun { NestedEnumAlias.value }
                val nestedClassAliasDInside by NestedEnumAlias.value
                val nestedClassAliasEInside by inPlaceDelegate { NestedEnumAlias.value }
                val nestedClassAliasFInside by nonInPlaceDelegate { NestedEnumAlias.value }

                init {
                    val memberAInit = value
                    val memberBInit = inPlaceRun { value }
                    val memberCInit = nonInPlaceRun { value }
                    val memberDInit by value
                    val memberEInit by inPlaceDelegate { value }
                    val memberFInit by nonInPlaceDelegate { value }

                    val companionNameReferenceAInit = Companion
                    val companionNameReferenceBInit = inPlaceRun { Companion }
                    val companionNameReferenceCInit = nonInPlaceRun { Companion }
                    val companionNameReferenceDInit by Companion
                    val companionNameReferenceEInit by inPlaceDelegate { Companion }
                    val companionNameReferenceFInit by nonInPlaceDelegate { Companion }

                    val classNameReferenceAInit = Enum
                    val classNameReferenceBInit = inPlaceRun { Enum }
                    val classNameReferenceCInit = nonInPlaceRun { Enum }
                    val classNameReferenceDInit by Enum
                    val classNameReferenceEInit by inPlaceDelegate { Enum }
                    val classNameReferenceFInit by nonInPlaceDelegate { Enum }

                    val classAndCompanionNameReferenceAInit = Enum.Companion
                    val classAndCompanionNameReferenceBInit = inPlaceRun { Enum.Companion }
                    val classAndCompanionNameReferenceCInit = nonInPlaceRun { Enum.Companion }
                    val classAndCompanionNameReferenceDInit by Enum.Companion
                    val classAndCompanionNameReferenceEInit by inPlaceDelegate { Enum.Companion }
                    val classAndCompanionNameReferenceFInit by nonInPlaceDelegate { Enum.Companion }

                    val companionAliasReferenceAInit = EnumCompanionAlias
                    val companionAliasReferenceBInit = inPlaceRun { EnumCompanionAlias }
                    val companionAliasReferenceCInit = nonInPlaceRun { EnumCompanionAlias }
                    val companionAliasReferenceDInit by EnumCompanionAlias
                    val companionAliasReferenceEInit by inPlaceDelegate { EnumCompanionAlias }
                    val companionAliasReferenceFInit by nonInPlaceDelegate { EnumCompanionAlias }

                    val nestedCompanionAliasReferenceAInit = NestedEnumCompanionAlias
                    val nestedCompanionAliasReferenceBInit = inPlaceRun { NestedEnumCompanionAlias }
                    val nestedCompanionAliasReferenceCInit = nonInPlaceRun { NestedEnumCompanionAlias }
                    val nestedCompanionAliasReferenceDInit by NestedEnumCompanionAlias
                    val nestedCompanionAliasReferenceEInit by inPlaceDelegate { NestedEnumCompanionAlias }
                    val nestedCompanionAliasReferenceFInit by nonInPlaceDelegate { NestedEnumCompanionAlias }

                    val classAliasReferenceAInit = EnumAlias
                    val classAliasReferenceBInit = inPlaceRun { EnumAlias }
                    val classAliasReferenceCInit = nonInPlaceRun { EnumAlias }
                    val classAliasReferenceDInit by EnumAlias
                    val classAliasReferenceEInit by inPlaceDelegate { EnumAlias }
                    val classAliasReferenceFInit by nonInPlaceDelegate { EnumAlias }

                    val nestedClassAliasReferenceAInit = NestedEnumAlias
                    val nestedClassAliasReferenceBInit = inPlaceRun { NestedEnumAlias }
                    val nestedClassAliasReferenceCInit = nonInPlaceRun { NestedEnumAlias }
                    val nestedClassAliasReferenceDInit by NestedEnumAlias
                    val nestedClassAliasReferenceEInit by inPlaceDelegate { NestedEnumAlias }
                    val nestedClassAliasReferenceFInit by nonInPlaceDelegate { NestedEnumAlias }

                    val companionNameAInit = Companion.value
                    val companionNameBInit = inPlaceRun { Companion.value }
                    val companionNameCInit = nonInPlaceRun { Companion.value }
                    val companionNameDInit by Companion.value
                    val companionNameEInit by inPlaceDelegate { Companion.value }
                    val companionNameFInit by nonInPlaceDelegate { Companion.value }

                    val classNameAInit = Enum.value
                    val classNameBInit = inPlaceRun { Enum.value }
                    val classNameCInit = nonInPlaceRun { Enum.value }
                    val classNameDInit by Enum.value
                    val classNameEInit by inPlaceDelegate { Enum.value }
                    val classNameFInit by nonInPlaceDelegate { Enum.value }

                    val classAndCompanionNameAInit = Enum.Companion.value
                    val classAndCompanionNameBInit = inPlaceRun { Enum.Companion.value }
                    val classAndCompanionNameCInit = nonInPlaceRun { Enum.Companion.value }
                    val classAndCompanionNameDInit by Enum.Companion.value
                    val classAndCompanionNameEInit by inPlaceDelegate { Enum.Companion.value }
                    val classAndCompanionNameFInit by nonInPlaceDelegate { Enum.Companion.value }

                    val companionAliasAInit = EnumCompanionAlias.value
                    val companionAliasBInit = inPlaceRun { EnumCompanionAlias.value }
                    val companionAliasCInit = nonInPlaceRun { EnumCompanionAlias.value }
                    val companionAliasDInit by EnumCompanionAlias.value
                    val companionAliasEInit by inPlaceDelegate { EnumCompanionAlias.value }
                    val companionAliasFInit by nonInPlaceDelegate { EnumCompanionAlias.value }

                    val nestedCompanionAliasAInit = NestedEnumCompanionAlias.value
                    val nestedCompanionAliasBInit = inPlaceRun { NestedEnumCompanionAlias.value }
                    val nestedCompanionAliasCInit = nonInPlaceRun { NestedEnumCompanionAlias.value }
                    val nestedCompanionAliasDInit by NestedEnumCompanionAlias.value
                    val nestedCompanionAliasEInit by inPlaceDelegate { NestedEnumCompanionAlias.value }
                    val nestedCompanionAliasFInit by nonInPlaceDelegate { NestedEnumCompanionAlias.value }

                    val classAliasAInit = EnumAlias.value
                    val classAliasBInit = inPlaceRun { EnumAlias.value }
                    val classAliasCInit = nonInPlaceRun { EnumAlias.value }
                    val classAliasDInit by EnumAlias.value
                    val classAliasEInit by inPlaceDelegate { EnumAlias.value }
                    val classAliasFInit by nonInPlaceDelegate { EnumAlias.value }

                    val nestedClassAliasAInit = NestedEnumAlias.value
                    val nestedClassAliasBInit = inPlaceRun { NestedEnumAlias.value }
                    val nestedClassAliasCInit = nonInPlaceRun { NestedEnumAlias.value }
                    val nestedClassAliasDInit by NestedEnumAlias.value
                    val nestedClassAliasEInit by inPlaceDelegate { NestedEnumAlias.value }
                    val nestedClassAliasFInit by nonInPlaceDelegate { NestedEnumAlias.value }
                }

                fun localFun() {
                    val memberA = value
                    val memberB = inPlaceRun { value }
                    val memberC = nonInPlaceRun { value }
                    val memberD by value
                    val memberE by inPlaceDelegate { value }
                    val memberF by nonInPlaceDelegate { value }

                    val companionNameReferenceA = Companion
                    val companionNameReferenceB = inPlaceRun { Companion }
                    val companionNameReferenceC = nonInPlaceRun { Companion }
                    val companionNameReferenceD by Companion
                    val companionNameReferenceE by inPlaceDelegate { Companion }
                    val companionNameReferenceF by nonInPlaceDelegate { Companion }

                    val classNameReferenceA = Enum
                    val classNameReferenceB = inPlaceRun { Enum }
                    val classNameReferenceC = nonInPlaceRun { Enum }
                    val classNameReferenceD by Enum
                    val classNameReferenceE by inPlaceDelegate { Enum }
                    val classNameReferenceF by nonInPlaceDelegate { Enum }

                    val classAndCompanionNameReferenceA = Enum.Companion
                    val classAndCompanionNameReferenceB = inPlaceRun { Enum.Companion }
                    val classAndCompanionNameReferenceC = nonInPlaceRun { Enum.Companion }
                    val classAndCompanionNameReferenceD by Enum.Companion
                    val classAndCompanionNameReferenceE by inPlaceDelegate { Enum.Companion }
                    val classAndCompanionNameReferenceF by nonInPlaceDelegate { Enum.Companion }

                    val companionAliasReferenceA = EnumCompanionAlias
                    val companionAliasReferenceB = inPlaceRun { EnumCompanionAlias }
                    val companionAliasReferenceC = nonInPlaceRun { EnumCompanionAlias }
                    val companionAliasReferenceD by EnumCompanionAlias
                    val companionAliasReferenceE by inPlaceDelegate { EnumCompanionAlias }
                    val companionAliasReferenceF by nonInPlaceDelegate { EnumCompanionAlias }

                    val nestedCompanionAliasReferenceA = NestedEnumCompanionAlias
                    val nestedCompanionAliasReferenceB = inPlaceRun { NestedEnumCompanionAlias }
                    val nestedCompanionAliasReferenceC = nonInPlaceRun { NestedEnumCompanionAlias }
                    val nestedCompanionAliasReferenceD by NestedEnumCompanionAlias
                    val nestedCompanionAliasReferenceE by inPlaceDelegate { NestedEnumCompanionAlias }
                    val nestedCompanionAliasReferenceF by nonInPlaceDelegate { NestedEnumCompanionAlias }

                    val classAliasReferenceA = EnumAlias
                    val classAliasReferenceB = inPlaceRun { EnumAlias }
                    val classAliasReferenceC = nonInPlaceRun { EnumAlias }
                    val classAliasReferenceD by EnumAlias
                    val classAliasReferenceE by inPlaceDelegate { EnumAlias }
                    val classAliasReferenceF by nonInPlaceDelegate { EnumAlias }

                    val nestedClassAliasReferenceA = NestedEnumAlias
                    val nestedClassAliasReferenceB = inPlaceRun { NestedEnumAlias }
                    val nestedClassAliasReferenceC = nonInPlaceRun { NestedEnumAlias }
                    val nestedClassAliasReferenceD by NestedEnumAlias
                    val nestedClassAliasReferenceE by inPlaceDelegate { NestedEnumAlias }
                    val nestedClassAliasReferenceF by nonInPlaceDelegate { NestedEnumAlias }

                    val companionNameA = Companion.value
                    val companionNameB = inPlaceRun { Companion.value }
                    val companionNameC = nonInPlaceRun { Companion.value }
                    val companionNameD by Companion.value
                    val companionNameE by inPlaceDelegate { Companion.value }
                    val companionNameF by nonInPlaceDelegate { Companion.value }

                    val classNameA = Enum.value
                    val classNameB = inPlaceRun { Enum.value }
                    val classNameC = nonInPlaceRun { Enum.value }
                    val classNameD by Enum.value
                    val classNameE by inPlaceDelegate { Enum.value }
                    val classNameF by nonInPlaceDelegate { Enum.value }

                    val classAndCompanionNameA = Enum.Companion.value
                    val classAndCompanionNameB = inPlaceRun { Enum.Companion.value }
                    val classAndCompanionNameC = nonInPlaceRun { Enum.Companion.value }
                    val classAndCompanionNameD by Enum.Companion.value
                    val classAndCompanionNameE by inPlaceDelegate { Enum.Companion.value }
                    val classAndCompanionNameF by nonInPlaceDelegate { Enum.Companion.value }

                    val companionAliasA = EnumCompanionAlias.value
                    val companionAliasB = inPlaceRun { EnumCompanionAlias.value }
                    val companionAliasC = nonInPlaceRun { EnumCompanionAlias.value }
                    val companionAliasD by EnumCompanionAlias.value
                    val companionAliasE by inPlaceDelegate { EnumCompanionAlias.value }
                    val companionAliasF by nonInPlaceDelegate { EnumCompanionAlias.value }

                    val nestedCompanionAliasA = NestedEnumCompanionAlias.value
                    val nestedCompanionAliasB = inPlaceRun { NestedEnumCompanionAlias.value }
                    val nestedCompanionAliasC = nonInPlaceRun { NestedEnumCompanionAlias.value }
                    val nestedCompanionAliasD by NestedEnumCompanionAlias.value
                    val nestedCompanionAliasE by inPlaceDelegate { NestedEnumCompanionAlias.value }
                    val nestedCompanionAliasF by nonInPlaceDelegate { NestedEnumCompanionAlias.value }

                    val classAliasA = EnumAlias.value
                    val classAliasB = inPlaceRun { EnumAlias.value }
                    val classAliasC = nonInPlaceRun { EnumAlias.value }
                    val classAliasD by EnumAlias.value
                    val classAliasE by inPlaceDelegate { EnumAlias.value }
                    val classAliasF by nonInPlaceDelegate { EnumAlias.value }

                    val nestedClassAliasA = NestedEnumAlias.value
                    val nestedClassAliasB = inPlaceRun { NestedEnumAlias.value }
                    val nestedClassAliasC = nonInPlaceRun { NestedEnumAlias.value }
                    val nestedClassAliasD by NestedEnumAlias.value
                    val nestedClassAliasE by inPlaceDelegate { NestedEnumAlias.value }
                    val nestedClassAliasF by nonInPlaceDelegate { NestedEnumAlias.value }
                }
            }
        }
    },
    D {
        init {
            val someObj = object {
                val memberAInside = <!UNINITIALIZED_ENUM_COMPANION!>value<!>
                val memberBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
                val memberCInside = nonInPlaceRun { value }
                val memberDInside by <!UNINITIALIZED_ENUM_COMPANION!>value<!>
                val memberEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
                val memberFInside by nonInPlaceDelegate { value }

                val companionNameReferenceAInside = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
                val companionNameReferenceBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
                val companionNameReferenceCInside = nonInPlaceRun { Companion }
                val companionNameReferenceDInside by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
                val companionNameReferenceEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
                val companionNameReferenceFInside by nonInPlaceDelegate { Companion }

                val classNameReferenceAInside = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>
                val classNameReferenceBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!> }
                val classNameReferenceCInside = nonInPlaceRun { Enum }
                val classNameReferenceDInside by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>
                val classNameReferenceEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!> }
                val classNameReferenceFInside by nonInPlaceDelegate { Enum }

                val classAndCompanionNameReferenceAInside = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
                val classAndCompanionNameReferenceBInside = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
                val classAndCompanionNameReferenceCInside = nonInPlaceRun { Enum.Companion }
                val classAndCompanionNameReferenceDInside by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
                val classAndCompanionNameReferenceEInside by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
                val classAndCompanionNameReferenceFInside by nonInPlaceDelegate { Enum.Companion }

                val companionAliasReferenceAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>
                val companionAliasReferenceBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!> }
                val companionAliasReferenceCInside = nonInPlaceRun { EnumCompanionAlias }
                val companionAliasReferenceDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>
                val companionAliasReferenceEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!> }
                val companionAliasReferenceFInside by nonInPlaceDelegate { EnumCompanionAlias }

                val nestedCompanionAliasReferenceAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>
                val nestedCompanionAliasReferenceBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!> }
                val nestedCompanionAliasReferenceCInside = nonInPlaceRun { NestedEnumCompanionAlias }
                val nestedCompanionAliasReferenceDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>
                val nestedCompanionAliasReferenceEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!> }
                val nestedCompanionAliasReferenceFInside by nonInPlaceDelegate { NestedEnumCompanionAlias }

                val classAliasReferenceAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>
                val classAliasReferenceBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!> }
                val classAliasReferenceCInside = nonInPlaceRun { EnumAlias }
                val classAliasReferenceDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>
                val classAliasReferenceEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!> }
                val classAliasReferenceFInside by nonInPlaceDelegate { EnumAlias }

                val nestedClassAliasReferenceAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>
                val nestedClassAliasReferenceBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!> }
                val nestedClassAliasReferenceCInside = nonInPlaceRun { NestedEnumAlias }
                val nestedClassAliasReferenceDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>
                val nestedClassAliasReferenceEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!> }
                val nestedClassAliasReferenceFInside by nonInPlaceDelegate { NestedEnumAlias }

                val companionNameAInside = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
                val companionNameBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
                val companionNameCInside = nonInPlaceRun { Companion.value }
                val companionNameDInside by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
                val companionNameEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
                val companionNameFInside by nonInPlaceDelegate { Companion.value }

                val classNameAInside = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value
                val classNameBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value }
                val classNameCInside = nonInPlaceRun { Enum.value }
                val classNameDInside by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value
                val classNameEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value }
                val classNameFInside by nonInPlaceDelegate { Enum.value }

                val classAndCompanionNameAInside = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
                val classAndCompanionNameBInside = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
                val classAndCompanionNameCInside = nonInPlaceRun { Enum.Companion.value }
                val classAndCompanionNameDInside by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
                val classAndCompanionNameEInside by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
                val classAndCompanionNameFInside by nonInPlaceDelegate { Enum.Companion.value }

                val companionAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value
                val companionAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value }
                val companionAliasCInside = nonInPlaceRun { EnumCompanionAlias.value }
                val companionAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value
                val companionAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value }
                val companionAliasFInside by nonInPlaceDelegate { EnumCompanionAlias.value }

                val nestedCompanionAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value
                val nestedCompanionAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value }
                val nestedCompanionAliasCInside = nonInPlaceRun { NestedEnumCompanionAlias.value }
                val nestedCompanionAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value
                val nestedCompanionAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value }
                val nestedCompanionAliasFInside by nonInPlaceDelegate { NestedEnumCompanionAlias.value }

                val classAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value
                val classAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value }
                val classAliasCInside = nonInPlaceRun { EnumAlias.value }
                val classAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value
                val classAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value }
                val classAliasFInside by nonInPlaceDelegate { EnumAlias.value }

                val nestedClassAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value
                val nestedClassAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value }
                val nestedClassAliasCInside = nonInPlaceRun { NestedEnumAlias.value }
                val nestedClassAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value
                val nestedClassAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value }
                val nestedClassAliasFInside by nonInPlaceDelegate { NestedEnumAlias.value }

                init {
                    val memberAInit = <!UNINITIALIZED_ENUM_COMPANION!>value<!>
                    val memberBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
                    val memberCInit = nonInPlaceRun { value }
                    val memberDInit by <!UNINITIALIZED_ENUM_COMPANION!>value<!>
                    val memberEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
                    val memberFInit by nonInPlaceDelegate { value }

                    val companionNameReferenceAInit = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
                    val companionNameReferenceBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
                    val companionNameReferenceCInit = nonInPlaceRun { Companion }
                    val companionNameReferenceDInit by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
                    val companionNameReferenceEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
                    val companionNameReferenceFInit by nonInPlaceDelegate { Companion }

                    val classNameReferenceAInit = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>
                    val classNameReferenceBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!> }
                    val classNameReferenceCInit = nonInPlaceRun { Enum }
                    val classNameReferenceDInit by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>
                    val classNameReferenceEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!> }
                    val classNameReferenceFInit by nonInPlaceDelegate { Enum }

                    val classAndCompanionNameReferenceAInit = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
                    val classAndCompanionNameReferenceBInit = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
                    val classAndCompanionNameReferenceCInit = nonInPlaceRun { Enum.Companion }
                    val classAndCompanionNameReferenceDInit by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
                    val classAndCompanionNameReferenceEInit by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
                    val classAndCompanionNameReferenceFInit by nonInPlaceDelegate { Enum.Companion }

                    val companionAliasReferenceAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>
                    val companionAliasReferenceBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!> }
                    val companionAliasReferenceCInit = nonInPlaceRun { EnumCompanionAlias }
                    val companionAliasReferenceDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>
                    val companionAliasReferenceEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!> }
                    val companionAliasReferenceFInit by nonInPlaceDelegate { EnumCompanionAlias }

                    val nestedCompanionAliasReferenceAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>
                    val nestedCompanionAliasReferenceBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!> }
                    val nestedCompanionAliasReferenceCInit = nonInPlaceRun { NestedEnumCompanionAlias }
                    val nestedCompanionAliasReferenceDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>
                    val nestedCompanionAliasReferenceEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!> }
                    val nestedCompanionAliasReferenceFInit by nonInPlaceDelegate { NestedEnumCompanionAlias }

                    val classAliasReferenceAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>
                    val classAliasReferenceBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!> }
                    val classAliasReferenceCInit = nonInPlaceRun { EnumAlias }
                    val classAliasReferenceDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>
                    val classAliasReferenceEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!> }
                    val classAliasReferenceFInit by nonInPlaceDelegate { EnumAlias }

                    val nestedClassAliasReferenceAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>
                    val nestedClassAliasReferenceBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!> }
                    val nestedClassAliasReferenceCInit = nonInPlaceRun { NestedEnumAlias }
                    val nestedClassAliasReferenceDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>
                    val nestedClassAliasReferenceEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!> }
                    val nestedClassAliasReferenceFInit by nonInPlaceDelegate { NestedEnumAlias }

                    val companionNameAInit = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
                    val companionNameBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
                    val companionNameCInit = nonInPlaceRun { Companion.value }
                    val companionNameDInit by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
                    val companionNameEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
                    val companionNameFInit by nonInPlaceDelegate { Companion.value }

                    val classNameAInit = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value
                    val classNameBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value }
                    val classNameCInit = nonInPlaceRun { Enum.value }
                    val classNameDInit by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value
                    val classNameEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value }
                    val classNameFInit by nonInPlaceDelegate { Enum.value }

                    val classAndCompanionNameAInit = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
                    val classAndCompanionNameBInit = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
                    val classAndCompanionNameCInit = nonInPlaceRun { Enum.Companion.value }
                    val classAndCompanionNameDInit by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
                    val classAndCompanionNameEInit by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
                    val classAndCompanionNameFInit by nonInPlaceDelegate { Enum.Companion.value }

                    val companionAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value
                    val companionAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value }
                    val companionAliasCInit = nonInPlaceRun { EnumCompanionAlias.value }
                    val companionAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value
                    val companionAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value }
                    val companionAliasFInit by nonInPlaceDelegate { EnumCompanionAlias.value }

                    val nestedCompanionAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value
                    val nestedCompanionAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value }
                    val nestedCompanionAliasCInit = nonInPlaceRun { NestedEnumCompanionAlias.value }
                    val nestedCompanionAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value
                    val nestedCompanionAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value }
                    val nestedCompanionAliasFInit by nonInPlaceDelegate { NestedEnumCompanionAlias.value }

                    val classAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value
                    val classAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value }
                    val classAliasCInit = nonInPlaceRun { EnumAlias.value }
                    val classAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value
                    val classAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value }
                    val classAliasFInit by nonInPlaceDelegate { EnumAlias.value }

                    val nestedClassAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value
                    val nestedClassAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value }
                    val nestedClassAliasCInit = nonInPlaceRun { NestedEnumAlias.value }
                    val nestedClassAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value
                    val nestedClassAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value }
                    val nestedClassAliasFInit by nonInPlaceDelegate { NestedEnumAlias.value }
                }

                fun localFun() {
                    val memberA = value
                    val memberB = inPlaceRun { value }
                    val memberC = nonInPlaceRun { value }
                    val memberD by value
                    val memberE by inPlaceDelegate { value }
                    val memberF by nonInPlaceDelegate { value }

                    val companionNameReferenceA = Companion
                    val companionNameReferenceB = inPlaceRun { Companion }
                    val companionNameReferenceC = nonInPlaceRun { Companion }
                    val companionNameReferenceD by Companion
                    val companionNameReferenceE by inPlaceDelegate { Companion }
                    val companionNameReferenceF by nonInPlaceDelegate { Companion }

                    val classNameReferenceA = Enum
                    val classNameReferenceB = inPlaceRun { Enum }
                    val classNameReferenceC = nonInPlaceRun { Enum }
                    val classNameReferenceD by Enum
                    val classNameReferenceE by inPlaceDelegate { Enum }
                    val classNameReferenceF by nonInPlaceDelegate { Enum }

                    val classAndCompanionNameReferenceA = Enum.Companion
                    val classAndCompanionNameReferenceB = inPlaceRun { Enum.Companion }
                    val classAndCompanionNameReferenceC = nonInPlaceRun { Enum.Companion }
                    val classAndCompanionNameReferenceD by Enum.Companion
                    val classAndCompanionNameReferenceE by inPlaceDelegate { Enum.Companion }
                    val classAndCompanionNameReferenceF by nonInPlaceDelegate { Enum.Companion }

                    val companionAliasReferenceA = EnumCompanionAlias
                    val companionAliasReferenceB = inPlaceRun { EnumCompanionAlias }
                    val companionAliasReferenceC = nonInPlaceRun { EnumCompanionAlias }
                    val companionAliasReferenceD by EnumCompanionAlias
                    val companionAliasReferenceE by inPlaceDelegate { EnumCompanionAlias }
                    val companionAliasReferenceF by nonInPlaceDelegate { EnumCompanionAlias }

                    val nestedCompanionAliasReferenceA = NestedEnumCompanionAlias
                    val nestedCompanionAliasReferenceB = inPlaceRun { NestedEnumCompanionAlias }
                    val nestedCompanionAliasReferenceC = nonInPlaceRun { NestedEnumCompanionAlias }
                    val nestedCompanionAliasReferenceD by NestedEnumCompanionAlias
                    val nestedCompanionAliasReferenceE by inPlaceDelegate { NestedEnumCompanionAlias }
                    val nestedCompanionAliasReferenceF by nonInPlaceDelegate { NestedEnumCompanionAlias }

                    val classAliasReferenceA = EnumAlias
                    val classAliasReferenceB = inPlaceRun { EnumAlias }
                    val classAliasReferenceC = nonInPlaceRun { EnumAlias }
                    val classAliasReferenceD by EnumAlias
                    val classAliasReferenceE by inPlaceDelegate { EnumAlias }
                    val classAliasReferenceF by nonInPlaceDelegate { EnumAlias }

                    val nestedClassAliasReferenceA = NestedEnumAlias
                    val nestedClassAliasReferenceB = inPlaceRun { NestedEnumAlias }
                    val nestedClassAliasReferenceC = nonInPlaceRun { NestedEnumAlias }
                    val nestedClassAliasReferenceD by NestedEnumAlias
                    val nestedClassAliasReferenceE by inPlaceDelegate { NestedEnumAlias }
                    val nestedClassAliasReferenceF by nonInPlaceDelegate { NestedEnumAlias }

                    val companionNameA = Companion.value
                    val companionNameB = inPlaceRun { Companion.value }
                    val companionNameC = nonInPlaceRun { Companion.value }
                    val companionNameD by Companion.value
                    val companionNameE by inPlaceDelegate { Companion.value }
                    val companionNameF by nonInPlaceDelegate { Companion.value }

                    val classNameA = Enum.value
                    val classNameB = inPlaceRun { Enum.value }
                    val classNameC = nonInPlaceRun { Enum.value }
                    val classNameD by Enum.value
                    val classNameE by inPlaceDelegate { Enum.value }
                    val classNameF by nonInPlaceDelegate { Enum.value }

                    val classAndCompanionNameA = Enum.Companion.value
                    val classAndCompanionNameB = inPlaceRun { Enum.Companion.value }
                    val classAndCompanionNameC = nonInPlaceRun { Enum.Companion.value }
                    val classAndCompanionNameD by Enum.Companion.value
                    val classAndCompanionNameE by inPlaceDelegate { Enum.Companion.value }
                    val classAndCompanionNameF by nonInPlaceDelegate { Enum.Companion.value }

                    val companionAliasA = EnumCompanionAlias.value
                    val companionAliasB = inPlaceRun { EnumCompanionAlias.value }
                    val companionAliasC = nonInPlaceRun { EnumCompanionAlias.value }
                    val companionAliasD by EnumCompanionAlias.value
                    val companionAliasE by inPlaceDelegate { EnumCompanionAlias.value }
                    val companionAliasF by nonInPlaceDelegate { EnumCompanionAlias.value }

                    val nestedCompanionAliasA = NestedEnumCompanionAlias.value
                    val nestedCompanionAliasB = inPlaceRun { NestedEnumCompanionAlias.value }
                    val nestedCompanionAliasC = nonInPlaceRun { NestedEnumCompanionAlias.value }
                    val nestedCompanionAliasD by NestedEnumCompanionAlias.value
                    val nestedCompanionAliasE by inPlaceDelegate { NestedEnumCompanionAlias.value }
                    val nestedCompanionAliasF by nonInPlaceDelegate { NestedEnumCompanionAlias.value }

                    val classAliasA = EnumAlias.value
                    val classAliasB = inPlaceRun { EnumAlias.value }
                    val classAliasC = nonInPlaceRun { EnumAlias.value }
                    val classAliasD by EnumAlias.value
                    val classAliasE by inPlaceDelegate { EnumAlias.value }
                    val classAliasF by nonInPlaceDelegate { EnumAlias.value }

                    val nestedClassAliasA = NestedEnumAlias.value
                    val nestedClassAliasB = inPlaceRun { NestedEnumAlias.value }
                    val nestedClassAliasC = nonInPlaceRun { NestedEnumAlias.value }
                    val nestedClassAliasD by NestedEnumAlias.value
                    val nestedClassAliasE by inPlaceDelegate { NestedEnumAlias.value }
                    val nestedClassAliasF by nonInPlaceDelegate { NestedEnumAlias.value }
                }
            }
        }
    }
    ;

    val memberA = <!UNINITIALIZED_ENUM_COMPANION!>value<!>
    val memberB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
    val memberC = nonInPlaceRun { value }
    val memberD by <!UNINITIALIZED_ENUM_COMPANION!>value<!>
    val memberE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
    val memberF by nonInPlaceDelegate { value }

    val companionNameReferenceA = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
    val companionNameReferenceB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
    val companionNameReferenceC = nonInPlaceRun { Companion }
    val companionNameReferenceD by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
    val companionNameReferenceE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
    val companionNameReferenceF by nonInPlaceDelegate { Companion }

    val classNameReferenceA = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>
    val classNameReferenceB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!> }
    val classNameReferenceC = nonInPlaceRun { Enum }
    val classNameReferenceD by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>
    val classNameReferenceE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!> }
    val classNameReferenceF by nonInPlaceDelegate { Enum }

    val classAndCompanionNameReferenceA = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
    val classAndCompanionNameReferenceB = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
    val classAndCompanionNameReferenceC = nonInPlaceRun { Enum.Companion }
    val classAndCompanionNameReferenceD by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
    val classAndCompanionNameReferenceE by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!> }
    val classAndCompanionNameReferenceF by nonInPlaceDelegate { Enum.Companion }

    val companionAliasReferenceA = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>
    val companionAliasReferenceB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!> }
    val companionAliasReferenceC = nonInPlaceRun { EnumCompanionAlias }
    val companionAliasReferenceD by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>
    val companionAliasReferenceE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!> }
    val companionAliasReferenceF by nonInPlaceDelegate { EnumCompanionAlias }

    val nestedCompanionAliasReferenceA = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>
    val nestedCompanionAliasReferenceB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!> }
    val nestedCompanionAliasReferenceC = nonInPlaceRun { NestedEnumCompanionAlias }
    val nestedCompanionAliasReferenceD by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>
    val nestedCompanionAliasReferenceE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!> }
    val nestedCompanionAliasReferenceF by nonInPlaceDelegate { NestedEnumCompanionAlias }

    val classAliasReferenceA = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>
    val classAliasReferenceB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!> }
    val classAliasReferenceC = nonInPlaceRun { EnumAlias }
    val classAliasReferenceD by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>
    val classAliasReferenceE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!> }
    val classAliasReferenceF by nonInPlaceDelegate { EnumAlias }

    val nestedClassAliasReferenceA = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>
    val nestedClassAliasReferenceB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!> }
    val nestedClassAliasReferenceC = nonInPlaceRun { NestedEnumAlias }
    val nestedClassAliasReferenceD by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>
    val nestedClassAliasReferenceE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!> }
    val nestedClassAliasReferenceF by nonInPlaceDelegate { NestedEnumAlias }

    val companionNameA = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
    val companionNameB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
    val companionNameC = nonInPlaceRun { Companion.value }
    val companionNameD by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
    val companionNameE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
    val companionNameF by nonInPlaceDelegate { Companion.value }

    val classNameA = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value
    val classNameB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value }
    val classNameC = nonInPlaceRun { Enum.value }
    val classNameD by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value
    val classNameE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.value }
    val classNameF by nonInPlaceDelegate { Enum.value }

    val classAndCompanionNameA = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
    val classAndCompanionNameB = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
    val classAndCompanionNameC = nonInPlaceRun { Enum.Companion.value }
    val classAndCompanionNameD by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value
    val classAndCompanionNameE by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value }
    val classAndCompanionNameF by nonInPlaceDelegate { Enum.Companion.value }

    val companionAliasA = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value
    val companionAliasB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value }
    val companionAliasC = nonInPlaceRun { EnumCompanionAlias.value }
    val companionAliasD by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value
    val companionAliasE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.value }
    val companionAliasF by nonInPlaceDelegate { EnumCompanionAlias.value }

    val nestedCompanionAliasA = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value
    val nestedCompanionAliasB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value }
    val nestedCompanionAliasC = nonInPlaceRun { NestedEnumCompanionAlias.value }
    val nestedCompanionAliasD by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value
    val nestedCompanionAliasE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.value }
    val nestedCompanionAliasF by nonInPlaceDelegate { NestedEnumCompanionAlias.value }

    val classAliasA = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value
    val classAliasB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value }
    val classAliasC = nonInPlaceRun { EnumAlias.value }
    val classAliasD by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value
    val classAliasE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.value }
    val classAliasF by nonInPlaceDelegate { EnumAlias.value }

    val nestedClassAliasA = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value
    val nestedClassAliasB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value }
    val nestedClassAliasC = nonInPlaceRun { NestedEnumAlias.value }
    val nestedClassAliasD by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value
    val nestedClassAliasE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.value }
    val nestedClassAliasF by nonInPlaceDelegate { NestedEnumAlias.value }

    companion object {
        val value = "value"
    }
}

enum class EnumWithConstructor(val a: String, val b: String, val c: String) {
    Member(
        a = <!UNINITIALIZED_ENUM_COMPANION!>value<!>,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> },
        c = nonInPlaceRun { value }
    ),
    CompanionName(
        a = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value },
        c = nonInPlaceRun { Companion.value }
    ),
    ClassName(
        a = <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructor<!>.value,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructor<!>.value },
        c = nonInPlaceRun { EnumWithConstructor.value }
    ),
    ClassAndCompanionName(
        a = EnumWithConstructor.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value,
        b = inPlaceRun { EnumWithConstructor.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.value },
        c = nonInPlaceRun { EnumWithConstructor.Companion.value }
    ),
    CompanionAlias(
        a = <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructorCompanionAlias<!>.value,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructorCompanionAlias<!>.value },
        c = nonInPlaceRun { EnumWithConstructorCompanionAlias.value }
    ),
    NestedCompanionAlias(
        a = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumWithConstructorCompanionAlias<!>.value,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumWithConstructorCompanionAlias<!>.value },
        c = nonInPlaceRun { NestedEnumWithConstructorCompanionAlias.value }
    ),
    ClassAlias(
        a = <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructorAlias<!>.value,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructorAlias<!>.value },
        c = nonInPlaceRun { EnumWithConstructorAlias.value }
    ),
    NestedClassAlias(
        a = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumWithConstructorAlias<!>.value,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumWithConstructorAlias<!>.value },
        c = nonInPlaceRun { NestedEnumWithConstructorAlias.value }
    );

    companion object {
        val value = "value"
    }
}

enum class EnumWithCompanionReferencesInConstructor(val a: Any, val b: Any, val c: Any) {
    CompanionNameReference(
        a = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!> },
        c = nonInPlaceRun { Companion }
    ),
    ClassNameReference(
        a = <!UNINITIALIZED_ENUM_COMPANION!>EnumWithCompanionReferencesInConstructor<!>,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumWithCompanionReferencesInConstructor<!> },
        c = nonInPlaceRun { EnumWithCompanionReferencesInConstructor }
    ),
    ClassAndCompanionNameReference(
        a = EnumWithCompanionReferencesInConstructor.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>,
        b = inPlaceRun { EnumWithCompanionReferencesInConstructor.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!> },
        c = nonInPlaceRun { EnumWithCompanionReferencesInConstructor.Companion }
    ),
    CompanionAliasReference(
        a = <!UNINITIALIZED_ENUM_COMPANION!>ReferencesCompanionAlias<!>,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>ReferencesCompanionAlias<!> },
        c = nonInPlaceRun { ReferencesCompanionAlias }
    ),
    NestedCompanionAliasReference(
        a = <!UNINITIALIZED_ENUM_COMPANION!>NestedReferencesCompanionAlias<!>,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedReferencesCompanionAlias<!> },
        c = nonInPlaceRun { NestedReferencesCompanionAlias }
    ),
    ClassAliasReference(
        a = <!UNINITIALIZED_ENUM_COMPANION!>ReferencesAlias<!>,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>ReferencesAlias<!> },
        c = nonInPlaceRun { ReferencesAlias }
    ),
    NestedClassAliasReference(
        a = <!UNINITIALIZED_ENUM_COMPANION!>NestedReferencesAlias<!>,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedReferencesAlias<!> },
        c = nonInPlaceRun { NestedReferencesAlias }
    );

    companion object
}

typealias EnumCompanionAlias = Enum.Companion
typealias NestedEnumCompanionAlias = EnumCompanionAlias
typealias EnumAlias = Enum
typealias NestedEnumAlias = EnumAlias

typealias EnumWithConstructorCompanionAlias = EnumWithConstructor.Companion
typealias NestedEnumWithConstructorCompanionAlias = EnumWithConstructorCompanionAlias
typealias EnumWithConstructorAlias = EnumWithConstructor
typealias NestedEnumWithConstructorAlias = EnumWithConstructorAlias

typealias ReferencesCompanionAlias = EnumWithCompanionReferencesInConstructor.Companion
typealias NestedReferencesCompanionAlias = ReferencesCompanionAlias
typealias ReferencesAlias = EnumWithCompanionReferencesInConstructor
typealias NestedReferencesAlias = ReferencesAlias

operator fun <T> T.provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, T> = ReadOnlyProperty { _, _ -> this }

inline fun <T> inPlaceRun(block: () -> T): T {
    contract { callsInPlace(block) }
    return block()
}

fun <T> nonInPlaceRun(block: () -> T): T {
    return block()
}

inline fun <T> inPlaceDelegate(block: () -> T): ReadOnlyProperty<Any?, T> {
    contract { callsInPlace(block) }
    val value = block()
    return ReadOnlyProperty { _, _ -> value }
}

fun <T> nonInPlaceDelegate(block: () -> T): ReadOnlyProperty<Any?, T> {
    return ReadOnlyProperty { _, _ -> block() }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, companionObject, contractCallsEffect, contracts,
enumDeclaration, enumEntry, funWithExtensionReceiver, functionDeclaration, functionalType, inline, lambdaLiteral,
localProperty, nullableType, objectDeclaration, operator, primaryConstructor, propertyDeclaration, propertyDelegate,
starProjection, stringLiteral, thisExpression, typeAliasDeclaration, typeParameter */
