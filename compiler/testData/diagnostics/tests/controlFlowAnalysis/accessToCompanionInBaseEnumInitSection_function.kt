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
        val memberAInside = <!UNINITIALIZED_ENUM_COMPANION!>foo<!>()
        val memberBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>foo<!>() }
        val memberCInside = nonInPlaceRun { foo() }
        val memberDInside by <!UNINITIALIZED_ENUM_COMPANION!>foo<!>()
        val memberEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>foo<!>() }
        val memberFInside by nonInPlaceDelegate { foo() }

        val companionNameAInside = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
        val companionNameBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
        val companionNameCInside = nonInPlaceRun { Companion.foo() }
        val companionNameDInside by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
        val companionNameEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
        val companionNameFInside by nonInPlaceDelegate { Companion.foo() }

        val classNameAInside = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo()
        val classNameBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo() }
        val classNameCInside = nonInPlaceRun { Enum.foo() }
        val classNameDInside by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo()
        val classNameEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo() }
        val classNameFInside by nonInPlaceDelegate { Enum.foo() }

        val classAndCompanionNameAInside = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
        val classAndCompanionNameBInside = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
        val classAndCompanionNameCInside = nonInPlaceRun { Enum.Companion.foo() }
        val classAndCompanionNameDInside by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
        val classAndCompanionNameEInside by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
        val classAndCompanionNameFInside by nonInPlaceDelegate { Enum.Companion.foo() }

        val companionAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo()
        val companionAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo() }
        val companionAliasCInside = nonInPlaceRun { EnumCompanionAlias.foo() }
        val companionAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo()
        val companionAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo() }
        val companionAliasFInside by nonInPlaceDelegate { EnumCompanionAlias.foo() }

        val nestedCompanionAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo()
        val nestedCompanionAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo() }
        val nestedCompanionAliasCInside = nonInPlaceRun { NestedEnumCompanionAlias.foo() }
        val nestedCompanionAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo()
        val nestedCompanionAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo() }
        val nestedCompanionAliasFInside by nonInPlaceDelegate { NestedEnumCompanionAlias.foo() }

        val classAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo()
        val classAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo() }
        val classAliasCInside = nonInPlaceRun { EnumAlias.foo() }
        val classAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo()
        val classAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo() }
        val classAliasFInside by nonInPlaceDelegate { EnumAlias.foo() }

        val nestedClassAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo()
        val nestedClassAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo() }
        val nestedClassAliasCInside = nonInPlaceRun { NestedEnumAlias.foo() }
        val nestedClassAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo()
        val nestedClassAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo() }
        val nestedClassAliasFInside by nonInPlaceDelegate { NestedEnumAlias.foo() }

        val companionNameInvokeAInside = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
        val companionNameInvokeBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
        val companionNameInvokeCInside = nonInPlaceRun { Companion() }
        val companionNameInvokeDInside by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
        val companionNameInvokeEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
        val companionNameInvokeFInside by nonInPlaceDelegate { Companion() }

        val classAndCompanionNameInvokeAInside = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
        val classAndCompanionNameInvokeBInside = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
        val classAndCompanionNameInvokeCInside = nonInPlaceRun { Enum.Companion() }
        val classAndCompanionNameInvokeDInside by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
        val classAndCompanionNameInvokeEInside by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
        val classAndCompanionNameInvokeFInside by nonInPlaceDelegate { Enum.Companion() }

        val companionNameExtensionInvokeAInside = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
        val companionNameExtensionInvokeBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
        val companionNameExtensionInvokeCInside = nonInPlaceRun { Companion(1) }
        val companionNameExtensionInvokeDInside by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
        val companionNameExtensionInvokeEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
        val companionNameExtensionInvokeFInside by nonInPlaceDelegate { Companion(1) }

        val classNameExtensionInvokeAInside = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1)
        val classNameExtensionInvokeBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1) }
        val classNameExtensionInvokeCInside = nonInPlaceRun { Enum(1) }
        val classNameExtensionInvokeDInside by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1)
        val classNameExtensionInvokeEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1) }
        val classNameExtensionInvokeFInside by nonInPlaceDelegate { Enum(1) }

        val classAndCompanionNameExtensionInvokeAInside = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
        val classAndCompanionNameExtensionInvokeBInside = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
        val classAndCompanionNameExtensionInvokeCInside = nonInPlaceRun { Enum.Companion(1) }
        val classAndCompanionNameExtensionInvokeDInside by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
        val classAndCompanionNameExtensionInvokeEInside by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
        val classAndCompanionNameExtensionInvokeFInside by nonInPlaceDelegate { Enum.Companion(1) }

        val companionAliasInvokeAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>()
        val companionAliasInvokeBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>() }
        val companionAliasInvokeCInside = nonInPlaceRun { EnumCompanionAlias() }
        val companionAliasInvokeDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>()
        val companionAliasInvokeEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>() }
        val companionAliasInvokeFInside by nonInPlaceDelegate { EnumCompanionAlias() }

        val nestedCompanionAliasInvokeAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>()
        val nestedCompanionAliasInvokeBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>() }
        val nestedCompanionAliasInvokeCInside = nonInPlaceRun { NestedEnumCompanionAlias() }
        val nestedCompanionAliasInvokeDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>()
        val nestedCompanionAliasInvokeEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>() }
        val nestedCompanionAliasInvokeFInside by nonInPlaceDelegate { NestedEnumCompanionAlias() }
    },
    B {
        init {
            val memberAInit = <!UNINITIALIZED_ENUM_COMPANION!>foo<!>()
            val memberBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>foo<!>() }
            val memberCInit = nonInPlaceRun { foo() }
            val memberDInit by <!UNINITIALIZED_ENUM_COMPANION!>foo<!>()
            val memberEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>foo<!>() }
            val memberFInit by nonInPlaceDelegate { foo() }

            val companionNameAInit = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
            val companionNameBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
            val companionNameCInit = nonInPlaceRun { Companion.foo() }
            val companionNameDInit by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
            val companionNameEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
            val companionNameFInit by nonInPlaceDelegate { Companion.foo() }

            val classNameAInit = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo()
            val classNameBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo() }
            val classNameCInit = nonInPlaceRun { Enum.foo() }
            val classNameDInit by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo()
            val classNameEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo() }
            val classNameFInit by nonInPlaceDelegate { Enum.foo() }

            val classAndCompanionNameAInit = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
            val classAndCompanionNameBInit = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
            val classAndCompanionNameCInit = nonInPlaceRun { Enum.Companion.foo() }
            val classAndCompanionNameDInit by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
            val classAndCompanionNameEInit by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
            val classAndCompanionNameFInit by nonInPlaceDelegate { Enum.Companion.foo() }

            val companionAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo()
            val companionAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo() }
            val companionAliasCInit = nonInPlaceRun { EnumCompanionAlias.foo() }
            val companionAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo()
            val companionAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo() }
            val companionAliasFInit by nonInPlaceDelegate { EnumCompanionAlias.foo() }

            val nestedCompanionAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo()
            val nestedCompanionAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo() }
            val nestedCompanionAliasCInit = nonInPlaceRun { NestedEnumCompanionAlias.foo() }
            val nestedCompanionAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo()
            val nestedCompanionAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo() }
            val nestedCompanionAliasFInit by nonInPlaceDelegate { NestedEnumCompanionAlias.foo() }

            val classAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo()
            val classAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo() }
            val classAliasCInit = nonInPlaceRun { EnumAlias.foo() }
            val classAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo()
            val classAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo() }
            val classAliasFInit by nonInPlaceDelegate { EnumAlias.foo() }

            val nestedClassAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo()
            val nestedClassAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo() }
            val nestedClassAliasCInit = nonInPlaceRun { NestedEnumAlias.foo() }
            val nestedClassAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo()
            val nestedClassAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo() }
            val nestedClassAliasFInit by nonInPlaceDelegate { NestedEnumAlias.foo() }

            val companionNameInvokeAInit = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
            val companionNameInvokeBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
            val companionNameInvokeCInit = nonInPlaceRun { Companion() }
            val companionNameInvokeDInit by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
            val companionNameInvokeEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
            val companionNameInvokeFInit by nonInPlaceDelegate { Companion() }

            val classAndCompanionNameInvokeAInit = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
            val classAndCompanionNameInvokeBInit = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
            val classAndCompanionNameInvokeCInit = nonInPlaceRun { Enum.Companion() }
            val classAndCompanionNameInvokeDInit by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
            val classAndCompanionNameInvokeEInit by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
            val classAndCompanionNameInvokeFInit by nonInPlaceDelegate { Enum.Companion() }

            val companionNameExtensionInvokeAInit = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
            val companionNameExtensionInvokeBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
            val companionNameExtensionInvokeCInit = nonInPlaceRun { Companion(1) }
            val companionNameExtensionInvokeDInit by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
            val companionNameExtensionInvokeEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
            val companionNameExtensionInvokeFInit by nonInPlaceDelegate { Companion(1) }

            val classNameExtensionInvokeAInit = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1)
            val classNameExtensionInvokeBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1) }
            val classNameExtensionInvokeCInit = nonInPlaceRun { Enum(1) }
            val classNameExtensionInvokeDInit by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1)
            val classNameExtensionInvokeEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1) }
            val classNameExtensionInvokeFInit by nonInPlaceDelegate { Enum(1) }

            val classAndCompanionNameExtensionInvokeAInit = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
            val classAndCompanionNameExtensionInvokeBInit = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
            val classAndCompanionNameExtensionInvokeCInit = nonInPlaceRun { Enum.Companion(1) }
            val classAndCompanionNameExtensionInvokeDInit by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
            val classAndCompanionNameExtensionInvokeEInit by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
            val classAndCompanionNameExtensionInvokeFInit by nonInPlaceDelegate { Enum.Companion(1) }

            val companionAliasInvokeAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>()
            val companionAliasInvokeBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>() }
            val companionAliasInvokeCInit = nonInPlaceRun { EnumCompanionAlias() }
            val companionAliasInvokeDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>()
            val companionAliasInvokeEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>() }
            val companionAliasInvokeFInit by nonInPlaceDelegate { EnumCompanionAlias() }

            val nestedCompanionAliasInvokeAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>()
            val nestedCompanionAliasInvokeBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>() }
            val nestedCompanionAliasInvokeCInit = nonInPlaceRun { NestedEnumCompanionAlias() }
            val nestedCompanionAliasInvokeDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>()
            val nestedCompanionAliasInvokeEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>() }
            val nestedCompanionAliasInvokeFInit by nonInPlaceDelegate { NestedEnumCompanionAlias() }
        }
    },
    C {
        init {
            class Local {
                val memberAInside = foo()
                val memberBInside = inPlaceRun { foo() }
                val memberCInside = nonInPlaceRun { foo() }
                val memberDInside by foo()
                val memberEInside by inPlaceDelegate { foo() }
                val memberFInside by nonInPlaceDelegate { foo() }

                val companionNameAInside = Companion.foo()
                val companionNameBInside = inPlaceRun { Companion.foo() }
                val companionNameCInside = nonInPlaceRun { Companion.foo() }
                val companionNameDInside by Companion.foo()
                val companionNameEInside by inPlaceDelegate { Companion.foo() }
                val companionNameFInside by nonInPlaceDelegate { Companion.foo() }

                val classNameAInside = Enum.foo()
                val classNameBInside = inPlaceRun { Enum.foo() }
                val classNameCInside = nonInPlaceRun { Enum.foo() }
                val classNameDInside by Enum.foo()
                val classNameEInside by inPlaceDelegate { Enum.foo() }
                val classNameFInside by nonInPlaceDelegate { Enum.foo() }

                val classAndCompanionNameAInside = Enum.Companion.foo()
                val classAndCompanionNameBInside = inPlaceRun { Enum.Companion.foo() }
                val classAndCompanionNameCInside = nonInPlaceRun { Enum.Companion.foo() }
                val classAndCompanionNameDInside by Enum.Companion.foo()
                val classAndCompanionNameEInside by inPlaceDelegate { Enum.Companion.foo() }
                val classAndCompanionNameFInside by nonInPlaceDelegate { Enum.Companion.foo() }

                val companionAliasAInside = EnumCompanionAlias.foo()
                val companionAliasBInside = inPlaceRun { EnumCompanionAlias.foo() }
                val companionAliasCInside = nonInPlaceRun { EnumCompanionAlias.foo() }
                val companionAliasDInside by EnumCompanionAlias.foo()
                val companionAliasEInside by inPlaceDelegate { EnumCompanionAlias.foo() }
                val companionAliasFInside by nonInPlaceDelegate { EnumCompanionAlias.foo() }

                val nestedCompanionAliasAInside = NestedEnumCompanionAlias.foo()
                val nestedCompanionAliasBInside = inPlaceRun { NestedEnumCompanionAlias.foo() }
                val nestedCompanionAliasCInside = nonInPlaceRun { NestedEnumCompanionAlias.foo() }
                val nestedCompanionAliasDInside by NestedEnumCompanionAlias.foo()
                val nestedCompanionAliasEInside by inPlaceDelegate { NestedEnumCompanionAlias.foo() }
                val nestedCompanionAliasFInside by nonInPlaceDelegate { NestedEnumCompanionAlias.foo() }

                val classAliasAInside = EnumAlias.foo()
                val classAliasBInside = inPlaceRun { EnumAlias.foo() }
                val classAliasCInside = nonInPlaceRun { EnumAlias.foo() }
                val classAliasDInside by EnumAlias.foo()
                val classAliasEInside by inPlaceDelegate { EnumAlias.foo() }
                val classAliasFInside by nonInPlaceDelegate { EnumAlias.foo() }

                val nestedClassAliasAInside = NestedEnumAlias.foo()
                val nestedClassAliasBInside = inPlaceRun { NestedEnumAlias.foo() }
                val nestedClassAliasCInside = nonInPlaceRun { NestedEnumAlias.foo() }
                val nestedClassAliasDInside by NestedEnumAlias.foo()
                val nestedClassAliasEInside by inPlaceDelegate { NestedEnumAlias.foo() }
                val nestedClassAliasFInside by nonInPlaceDelegate { NestedEnumAlias.foo() }

                val companionNameInvokeAInside = Companion()
                val companionNameInvokeBInside = inPlaceRun { Companion() }
                val companionNameInvokeCInside = nonInPlaceRun { Companion() }
                val companionNameInvokeDInside by Companion()
                val companionNameInvokeEInside by inPlaceDelegate { Companion() }
                val companionNameInvokeFInside by nonInPlaceDelegate { Companion() }

                val classAndCompanionNameInvokeAInside = Enum.Companion()
                val classAndCompanionNameInvokeBInside = inPlaceRun { Enum.Companion() }
                val classAndCompanionNameInvokeCInside = nonInPlaceRun { Enum.Companion() }
                val classAndCompanionNameInvokeDInside by Enum.Companion()
                val classAndCompanionNameInvokeEInside by inPlaceDelegate { Enum.Companion() }
                val classAndCompanionNameInvokeFInside by nonInPlaceDelegate { Enum.Companion() }

                val companionNameExtensionInvokeAInside = Companion(1)
                val companionNameExtensionInvokeBInside = inPlaceRun { Companion(1) }
                val companionNameExtensionInvokeCInside = nonInPlaceRun { Companion(1) }
                val companionNameExtensionInvokeDInside by Companion(1)
                val companionNameExtensionInvokeEInside by inPlaceDelegate { Companion(1) }
                val companionNameExtensionInvokeFInside by nonInPlaceDelegate { Companion(1) }

                val classNameExtensionInvokeAInside = Enum(1)
                val classNameExtensionInvokeBInside = inPlaceRun { Enum(1) }
                val classNameExtensionInvokeCInside = nonInPlaceRun { Enum(1) }
                val classNameExtensionInvokeDInside by Enum(1)
                val classNameExtensionInvokeEInside by inPlaceDelegate { Enum(1) }
                val classNameExtensionInvokeFInside by nonInPlaceDelegate { Enum(1) }

                val classAndCompanionNameExtensionInvokeAInside = Enum.Companion(1)
                val classAndCompanionNameExtensionInvokeBInside = inPlaceRun { Enum.Companion(1) }
                val classAndCompanionNameExtensionInvokeCInside = nonInPlaceRun { Enum.Companion(1) }
                val classAndCompanionNameExtensionInvokeDInside by Enum.Companion(1)
                val classAndCompanionNameExtensionInvokeEInside by inPlaceDelegate { Enum.Companion(1) }
                val classAndCompanionNameExtensionInvokeFInside by nonInPlaceDelegate { Enum.Companion(1) }

                val companionAliasInvokeAInside = EnumCompanionAlias()
                val companionAliasInvokeBInside = inPlaceRun { EnumCompanionAlias() }
                val companionAliasInvokeCInside = nonInPlaceRun { EnumCompanionAlias() }
                val companionAliasInvokeDInside by EnumCompanionAlias()
                val companionAliasInvokeEInside by inPlaceDelegate { EnumCompanionAlias() }
                val companionAliasInvokeFInside by nonInPlaceDelegate { EnumCompanionAlias() }

                val nestedCompanionAliasInvokeAInside = NestedEnumCompanionAlias()
                val nestedCompanionAliasInvokeBInside = inPlaceRun { NestedEnumCompanionAlias() }
                val nestedCompanionAliasInvokeCInside = nonInPlaceRun { NestedEnumCompanionAlias() }
                val nestedCompanionAliasInvokeDInside by NestedEnumCompanionAlias()
                val nestedCompanionAliasInvokeEInside by inPlaceDelegate { NestedEnumCompanionAlias() }
                val nestedCompanionAliasInvokeFInside by nonInPlaceDelegate { NestedEnumCompanionAlias() }

                init {
                    val memberAInit = foo()
                    val memberBInit = inPlaceRun { foo() }
                    val memberCInit = nonInPlaceRun { foo() }
                    val memberDInit by foo()
                    val memberEInit by inPlaceDelegate { foo() }
                    val memberFInit by nonInPlaceDelegate { foo() }

                    val companionNameAInit = Companion.foo()
                    val companionNameBInit = inPlaceRun { Companion.foo() }
                    val companionNameCInit = nonInPlaceRun { Companion.foo() }
                    val companionNameDInit by Companion.foo()
                    val companionNameEInit by inPlaceDelegate { Companion.foo() }
                    val companionNameFInit by nonInPlaceDelegate { Companion.foo() }

                    val classNameAInit = Enum.foo()
                    val classNameBInit = inPlaceRun { Enum.foo() }
                    val classNameCInit = nonInPlaceRun { Enum.foo() }
                    val classNameDInit by Enum.foo()
                    val classNameEInit by inPlaceDelegate { Enum.foo() }
                    val classNameFInit by nonInPlaceDelegate { Enum.foo() }

                    val classAndCompanionNameAInit = Enum.Companion.foo()
                    val classAndCompanionNameBInit = inPlaceRun { Enum.Companion.foo() }
                    val classAndCompanionNameCInit = nonInPlaceRun { Enum.Companion.foo() }
                    val classAndCompanionNameDInit by Enum.Companion.foo()
                    val classAndCompanionNameEInit by inPlaceDelegate { Enum.Companion.foo() }
                    val classAndCompanionNameFInit by nonInPlaceDelegate { Enum.Companion.foo() }

                    val companionAliasAInit = EnumCompanionAlias.foo()
                    val companionAliasBInit = inPlaceRun { EnumCompanionAlias.foo() }
                    val companionAliasCInit = nonInPlaceRun { EnumCompanionAlias.foo() }
                    val companionAliasDInit by EnumCompanionAlias.foo()
                    val companionAliasEInit by inPlaceDelegate { EnumCompanionAlias.foo() }
                    val companionAliasFInit by nonInPlaceDelegate { EnumCompanionAlias.foo() }

                    val nestedCompanionAliasAInit = NestedEnumCompanionAlias.foo()
                    val nestedCompanionAliasBInit = inPlaceRun { NestedEnumCompanionAlias.foo() }
                    val nestedCompanionAliasCInit = nonInPlaceRun { NestedEnumCompanionAlias.foo() }
                    val nestedCompanionAliasDInit by NestedEnumCompanionAlias.foo()
                    val nestedCompanionAliasEInit by inPlaceDelegate { NestedEnumCompanionAlias.foo() }
                    val nestedCompanionAliasFInit by nonInPlaceDelegate { NestedEnumCompanionAlias.foo() }

                    val classAliasAInit = EnumAlias.foo()
                    val classAliasBInit = inPlaceRun { EnumAlias.foo() }
                    val classAliasCInit = nonInPlaceRun { EnumAlias.foo() }
                    val classAliasDInit by EnumAlias.foo()
                    val classAliasEInit by inPlaceDelegate { EnumAlias.foo() }
                    val classAliasFInit by nonInPlaceDelegate { EnumAlias.foo() }

                    val nestedClassAliasAInit = NestedEnumAlias.foo()
                    val nestedClassAliasBInit = inPlaceRun { NestedEnumAlias.foo() }
                    val nestedClassAliasCInit = nonInPlaceRun { NestedEnumAlias.foo() }
                    val nestedClassAliasDInit by NestedEnumAlias.foo()
                    val nestedClassAliasEInit by inPlaceDelegate { NestedEnumAlias.foo() }
                    val nestedClassAliasFInit by nonInPlaceDelegate { NestedEnumAlias.foo() }

                    val companionNameInvokeAInit = Companion()
                    val companionNameInvokeBInit = inPlaceRun { Companion() }
                    val companionNameInvokeCInit = nonInPlaceRun { Companion() }
                    val companionNameInvokeDInit by Companion()
                    val companionNameInvokeEInit by inPlaceDelegate { Companion() }
                    val companionNameInvokeFInit by nonInPlaceDelegate { Companion() }

                    val classAndCompanionNameInvokeAInit = Enum.Companion()
                    val classAndCompanionNameInvokeBInit = inPlaceRun { Enum.Companion() }
                    val classAndCompanionNameInvokeCInit = nonInPlaceRun { Enum.Companion() }
                    val classAndCompanionNameInvokeDInit by Enum.Companion()
                    val classAndCompanionNameInvokeEInit by inPlaceDelegate { Enum.Companion() }
                    val classAndCompanionNameInvokeFInit by nonInPlaceDelegate { Enum.Companion() }

                    val companionNameExtensionInvokeAInit = Companion(1)
                    val companionNameExtensionInvokeBInit = inPlaceRun { Companion(1) }
                    val companionNameExtensionInvokeCInit = nonInPlaceRun { Companion(1) }
                    val companionNameExtensionInvokeDInit by Companion(1)
                    val companionNameExtensionInvokeEInit by inPlaceDelegate { Companion(1) }
                    val companionNameExtensionInvokeFInit by nonInPlaceDelegate { Companion(1) }

                    val classNameExtensionInvokeAInit = Enum(1)
                    val classNameExtensionInvokeBInit = inPlaceRun { Enum(1) }
                    val classNameExtensionInvokeCInit = nonInPlaceRun { Enum(1) }
                    val classNameExtensionInvokeDInit by Enum(1)
                    val classNameExtensionInvokeEInit by inPlaceDelegate { Enum(1) }
                    val classNameExtensionInvokeFInit by nonInPlaceDelegate { Enum(1) }

                    val classAndCompanionNameExtensionInvokeAInit = Enum.Companion(1)
                    val classAndCompanionNameExtensionInvokeBInit = inPlaceRun { Enum.Companion(1) }
                    val classAndCompanionNameExtensionInvokeCInit = nonInPlaceRun { Enum.Companion(1) }
                    val classAndCompanionNameExtensionInvokeDInit by Enum.Companion(1)
                    val classAndCompanionNameExtensionInvokeEInit by inPlaceDelegate { Enum.Companion(1) }
                    val classAndCompanionNameExtensionInvokeFInit by nonInPlaceDelegate { Enum.Companion(1) }

                    val companionAliasInvokeAInit = EnumCompanionAlias()
                    val companionAliasInvokeBInit = inPlaceRun { EnumCompanionAlias() }
                    val companionAliasInvokeCInit = nonInPlaceRun { EnumCompanionAlias() }
                    val companionAliasInvokeDInit by EnumCompanionAlias()
                    val companionAliasInvokeEInit by inPlaceDelegate { EnumCompanionAlias() }
                    val companionAliasInvokeFInit by nonInPlaceDelegate { EnumCompanionAlias() }

                    val nestedCompanionAliasInvokeAInit = NestedEnumCompanionAlias()
                    val nestedCompanionAliasInvokeBInit = inPlaceRun { NestedEnumCompanionAlias() }
                    val nestedCompanionAliasInvokeCInit = nonInPlaceRun { NestedEnumCompanionAlias() }
                    val nestedCompanionAliasInvokeDInit by NestedEnumCompanionAlias()
                    val nestedCompanionAliasInvokeEInit by inPlaceDelegate { NestedEnumCompanionAlias() }
                    val nestedCompanionAliasInvokeFInit by nonInPlaceDelegate { NestedEnumCompanionAlias() }
                }

                fun localFun() {
                    val memberA = foo()
                    val memberB = inPlaceRun { foo() }
                    val memberC = nonInPlaceRun { foo() }
                    val memberD by foo()
                    val memberE by inPlaceDelegate { foo() }
                    val memberF by nonInPlaceDelegate { foo() }

                    val companionNameA = Companion.foo()
                    val companionNameB = inPlaceRun { Companion.foo() }
                    val companionNameC = nonInPlaceRun { Companion.foo() }
                    val companionNameD by Companion.foo()
                    val companionNameE by inPlaceDelegate { Companion.foo() }
                    val companionNameF by nonInPlaceDelegate { Companion.foo() }

                    val classNameA = Enum.foo()
                    val classNameB = inPlaceRun { Enum.foo() }
                    val classNameC = nonInPlaceRun { Enum.foo() }
                    val classNameD by Enum.foo()
                    val classNameE by inPlaceDelegate { Enum.foo() }
                    val classNameF by nonInPlaceDelegate { Enum.foo() }

                    val classAndCompanionNameA = Enum.Companion.foo()
                    val classAndCompanionNameB = inPlaceRun { Enum.Companion.foo() }
                    val classAndCompanionNameC = nonInPlaceRun { Enum.Companion.foo() }
                    val classAndCompanionNameD by Enum.Companion.foo()
                    val classAndCompanionNameE by inPlaceDelegate { Enum.Companion.foo() }
                    val classAndCompanionNameF by nonInPlaceDelegate { Enum.Companion.foo() }

                    val companionAliasA = EnumCompanionAlias.foo()
                    val companionAliasB = inPlaceRun { EnumCompanionAlias.foo() }
                    val companionAliasC = nonInPlaceRun { EnumCompanionAlias.foo() }
                    val companionAliasD by EnumCompanionAlias.foo()
                    val companionAliasE by inPlaceDelegate { EnumCompanionAlias.foo() }
                    val companionAliasF by nonInPlaceDelegate { EnumCompanionAlias.foo() }

                    val nestedCompanionAliasA = NestedEnumCompanionAlias.foo()
                    val nestedCompanionAliasB = inPlaceRun { NestedEnumCompanionAlias.foo() }
                    val nestedCompanionAliasC = nonInPlaceRun { NestedEnumCompanionAlias.foo() }
                    val nestedCompanionAliasD by NestedEnumCompanionAlias.foo()
                    val nestedCompanionAliasE by inPlaceDelegate { NestedEnumCompanionAlias.foo() }
                    val nestedCompanionAliasF by nonInPlaceDelegate { NestedEnumCompanionAlias.foo() }

                    val classAliasA = EnumAlias.foo()
                    val classAliasB = inPlaceRun { EnumAlias.foo() }
                    val classAliasC = nonInPlaceRun { EnumAlias.foo() }
                    val classAliasD by EnumAlias.foo()
                    val classAliasE by inPlaceDelegate { EnumAlias.foo() }
                    val classAliasF by nonInPlaceDelegate { EnumAlias.foo() }

                    val nestedClassAliasA = NestedEnumAlias.foo()
                    val nestedClassAliasB = inPlaceRun { NestedEnumAlias.foo() }
                    val nestedClassAliasC = nonInPlaceRun { NestedEnumAlias.foo() }
                    val nestedClassAliasD by NestedEnumAlias.foo()
                    val nestedClassAliasE by inPlaceDelegate { NestedEnumAlias.foo() }
                    val nestedClassAliasF by nonInPlaceDelegate { NestedEnumAlias.foo() }

                    val companionNameInvokeA = Companion()
                    val companionNameInvokeB = inPlaceRun { Companion() }
                    val companionNameInvokeC = nonInPlaceRun { Companion() }
                    val companionNameInvokeD by Companion()
                    val companionNameInvokeE by inPlaceDelegate { Companion() }
                    val companionNameInvokeF by nonInPlaceDelegate { Companion() }

                    val classAndCompanionNameInvokeA = Enum.Companion()
                    val classAndCompanionNameInvokeB = inPlaceRun { Enum.Companion() }
                    val classAndCompanionNameInvokeC = nonInPlaceRun { Enum.Companion() }
                    val classAndCompanionNameInvokeD by Enum.Companion()
                    val classAndCompanionNameInvokeE by inPlaceDelegate { Enum.Companion() }
                    val classAndCompanionNameInvokeF by nonInPlaceDelegate { Enum.Companion() }

                    val companionNameExtensionInvokeA = Companion(1)
                    val companionNameExtensionInvokeB = inPlaceRun { Companion(1) }
                    val companionNameExtensionInvokeC = nonInPlaceRun { Companion(1) }
                    val companionNameExtensionInvokeD by Companion(1)
                    val companionNameExtensionInvokeE by inPlaceDelegate { Companion(1) }
                    val companionNameExtensionInvokeF by nonInPlaceDelegate { Companion(1) }

                    val classNameExtensionInvokeA = Enum(1)
                    val classNameExtensionInvokeB = inPlaceRun { Enum(1) }
                    val classNameExtensionInvokeC = nonInPlaceRun { Enum(1) }
                    val classNameExtensionInvokeD by Enum(1)
                    val classNameExtensionInvokeE by inPlaceDelegate { Enum(1) }
                    val classNameExtensionInvokeF by nonInPlaceDelegate { Enum(1) }

                    val classAndCompanionNameExtensionInvokeA = Enum.Companion(1)
                    val classAndCompanionNameExtensionInvokeB = inPlaceRun { Enum.Companion(1) }
                    val classAndCompanionNameExtensionInvokeC = nonInPlaceRun { Enum.Companion(1) }
                    val classAndCompanionNameExtensionInvokeD by Enum.Companion(1)
                    val classAndCompanionNameExtensionInvokeE by inPlaceDelegate { Enum.Companion(1) }
                    val classAndCompanionNameExtensionInvokeF by nonInPlaceDelegate { Enum.Companion(1) }

                    val companionAliasInvokeA = EnumCompanionAlias()
                    val companionAliasInvokeB = inPlaceRun { EnumCompanionAlias() }
                    val companionAliasInvokeC = nonInPlaceRun { EnumCompanionAlias() }
                    val companionAliasInvokeD by EnumCompanionAlias()
                    val companionAliasInvokeE by inPlaceDelegate { EnumCompanionAlias() }
                    val companionAliasInvokeF by nonInPlaceDelegate { EnumCompanionAlias() }

                    val nestedCompanionAliasInvokeA = NestedEnumCompanionAlias()
                    val nestedCompanionAliasInvokeB = inPlaceRun { NestedEnumCompanionAlias() }
                    val nestedCompanionAliasInvokeC = nonInPlaceRun { NestedEnumCompanionAlias() }
                    val nestedCompanionAliasInvokeD by NestedEnumCompanionAlias()
                    val nestedCompanionAliasInvokeE by inPlaceDelegate { NestedEnumCompanionAlias() }
                    val nestedCompanionAliasInvokeF by nonInPlaceDelegate { NestedEnumCompanionAlias() }
                }
            }
        }
    },
    D {
        init {
            val someObj = object {
                val memberAInside = <!UNINITIALIZED_ENUM_COMPANION!>foo<!>()
                val memberBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>foo<!>() }
                val memberCInside = nonInPlaceRun { foo() }
                val memberDInside by <!UNINITIALIZED_ENUM_COMPANION!>foo<!>()
                val memberEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>foo<!>() }
                val memberFInside by nonInPlaceDelegate { foo() }

                val companionNameAInside = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
                val companionNameBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
                val companionNameCInside = nonInPlaceRun { Companion.foo() }
                val companionNameDInside by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
                val companionNameEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
                val companionNameFInside by nonInPlaceDelegate { Companion.foo() }

                val classNameAInside = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo()
                val classNameBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo() }
                val classNameCInside = nonInPlaceRun { Enum.foo() }
                val classNameDInside by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo()
                val classNameEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo() }
                val classNameFInside by nonInPlaceDelegate { Enum.foo() }

                val classAndCompanionNameAInside = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
                val classAndCompanionNameBInside = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
                val classAndCompanionNameCInside = nonInPlaceRun { Enum.Companion.foo() }
                val classAndCompanionNameDInside by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
                val classAndCompanionNameEInside by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
                val classAndCompanionNameFInside by nonInPlaceDelegate { Enum.Companion.foo() }

                val companionAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo()
                val companionAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo() }
                val companionAliasCInside = nonInPlaceRun { EnumCompanionAlias.foo() }
                val companionAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo()
                val companionAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo() }
                val companionAliasFInside by nonInPlaceDelegate { EnumCompanionAlias.foo() }

                val nestedCompanionAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo()
                val nestedCompanionAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo() }
                val nestedCompanionAliasCInside = nonInPlaceRun { NestedEnumCompanionAlias.foo() }
                val nestedCompanionAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo()
                val nestedCompanionAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo() }
                val nestedCompanionAliasFInside by nonInPlaceDelegate { NestedEnumCompanionAlias.foo() }

                val classAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo()
                val classAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo() }
                val classAliasCInside = nonInPlaceRun { EnumAlias.foo() }
                val classAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo()
                val classAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo() }
                val classAliasFInside by nonInPlaceDelegate { EnumAlias.foo() }

                val nestedClassAliasAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo()
                val nestedClassAliasBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo() }
                val nestedClassAliasCInside = nonInPlaceRun { NestedEnumAlias.foo() }
                val nestedClassAliasDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo()
                val nestedClassAliasEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo() }
                val nestedClassAliasFInside by nonInPlaceDelegate { NestedEnumAlias.foo() }

                val companionNameInvokeAInside = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
                val companionNameInvokeBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
                val companionNameInvokeCInside = nonInPlaceRun { Companion() }
                val companionNameInvokeDInside by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
                val companionNameInvokeEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
                val companionNameInvokeFInside by nonInPlaceDelegate { Companion() }

                val classAndCompanionNameInvokeAInside = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
                val classAndCompanionNameInvokeBInside = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
                val classAndCompanionNameInvokeCInside = nonInPlaceRun { Enum.Companion() }
                val classAndCompanionNameInvokeDInside by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
                val classAndCompanionNameInvokeEInside by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
                val classAndCompanionNameInvokeFInside by nonInPlaceDelegate { Enum.Companion() }

                val companionNameExtensionInvokeAInside = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
                val companionNameExtensionInvokeBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
                val companionNameExtensionInvokeCInside = nonInPlaceRun { Companion(1) }
                val companionNameExtensionInvokeDInside by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
                val companionNameExtensionInvokeEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
                val companionNameExtensionInvokeFInside by nonInPlaceDelegate { Companion(1) }

                val classNameExtensionInvokeAInside = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1)
                val classNameExtensionInvokeBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1) }
                val classNameExtensionInvokeCInside = nonInPlaceRun { Enum(1) }
                val classNameExtensionInvokeDInside by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1)
                val classNameExtensionInvokeEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1) }
                val classNameExtensionInvokeFInside by nonInPlaceDelegate { Enum(1) }

                val classAndCompanionNameExtensionInvokeAInside = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
                val classAndCompanionNameExtensionInvokeBInside = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
                val classAndCompanionNameExtensionInvokeCInside = nonInPlaceRun { Enum.Companion(1) }
                val classAndCompanionNameExtensionInvokeDInside by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
                val classAndCompanionNameExtensionInvokeEInside by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
                val classAndCompanionNameExtensionInvokeFInside by nonInPlaceDelegate { Enum.Companion(1) }

                val companionAliasInvokeAInside = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>()
                val companionAliasInvokeBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>() }
                val companionAliasInvokeCInside = nonInPlaceRun { EnumCompanionAlias() }
                val companionAliasInvokeDInside by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>()
                val companionAliasInvokeEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>() }
                val companionAliasInvokeFInside by nonInPlaceDelegate { EnumCompanionAlias() }

                val nestedCompanionAliasInvokeAInside = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>()
                val nestedCompanionAliasInvokeBInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>() }
                val nestedCompanionAliasInvokeCInside = nonInPlaceRun { NestedEnumCompanionAlias() }
                val nestedCompanionAliasInvokeDInside by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>()
                val nestedCompanionAliasInvokeEInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>() }
                val nestedCompanionAliasInvokeFInside by nonInPlaceDelegate { NestedEnumCompanionAlias() }

                init {
                    val memberAInit = <!UNINITIALIZED_ENUM_COMPANION!>foo<!>()
                    val memberBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>foo<!>() }
                    val memberCInit = nonInPlaceRun { foo() }
                    val memberDInit by <!UNINITIALIZED_ENUM_COMPANION!>foo<!>()
                    val memberEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>foo<!>() }
                    val memberFInit by nonInPlaceDelegate { foo() }

                    val companionNameAInit = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
                    val companionNameBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
                    val companionNameCInit = nonInPlaceRun { Companion.foo() }
                    val companionNameDInit by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
                    val companionNameEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
                    val companionNameFInit by nonInPlaceDelegate { Companion.foo() }

                    val classNameAInit = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo()
                    val classNameBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo() }
                    val classNameCInit = nonInPlaceRun { Enum.foo() }
                    val classNameDInit by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo()
                    val classNameEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo() }
                    val classNameFInit by nonInPlaceDelegate { Enum.foo() }

                    val classAndCompanionNameAInit = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
                    val classAndCompanionNameBInit = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
                    val classAndCompanionNameCInit = nonInPlaceRun { Enum.Companion.foo() }
                    val classAndCompanionNameDInit by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
                    val classAndCompanionNameEInit by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
                    val classAndCompanionNameFInit by nonInPlaceDelegate { Enum.Companion.foo() }

                    val companionAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo()
                    val companionAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo() }
                    val companionAliasCInit = nonInPlaceRun { EnumCompanionAlias.foo() }
                    val companionAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo()
                    val companionAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo() }
                    val companionAliasFInit by nonInPlaceDelegate { EnumCompanionAlias.foo() }

                    val nestedCompanionAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo()
                    val nestedCompanionAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo() }
                    val nestedCompanionAliasCInit = nonInPlaceRun { NestedEnumCompanionAlias.foo() }
                    val nestedCompanionAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo()
                    val nestedCompanionAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo() }
                    val nestedCompanionAliasFInit by nonInPlaceDelegate { NestedEnumCompanionAlias.foo() }

                    val classAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo()
                    val classAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo() }
                    val classAliasCInit = nonInPlaceRun { EnumAlias.foo() }
                    val classAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo()
                    val classAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo() }
                    val classAliasFInit by nonInPlaceDelegate { EnumAlias.foo() }

                    val nestedClassAliasAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo()
                    val nestedClassAliasBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo() }
                    val nestedClassAliasCInit = nonInPlaceRun { NestedEnumAlias.foo() }
                    val nestedClassAliasDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo()
                    val nestedClassAliasEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo() }
                    val nestedClassAliasFInit by nonInPlaceDelegate { NestedEnumAlias.foo() }

                    val companionNameInvokeAInit = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
                    val companionNameInvokeBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
                    val companionNameInvokeCInit = nonInPlaceRun { Companion() }
                    val companionNameInvokeDInit by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
                    val companionNameInvokeEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
                    val companionNameInvokeFInit by nonInPlaceDelegate { Companion() }

                    val classAndCompanionNameInvokeAInit = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
                    val classAndCompanionNameInvokeBInit = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
                    val classAndCompanionNameInvokeCInit = nonInPlaceRun { Enum.Companion() }
                    val classAndCompanionNameInvokeDInit by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
                    val classAndCompanionNameInvokeEInit by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
                    val classAndCompanionNameInvokeFInit by nonInPlaceDelegate { Enum.Companion() }

                    val companionNameExtensionInvokeAInit = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
                    val companionNameExtensionInvokeBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
                    val companionNameExtensionInvokeCInit = nonInPlaceRun { Companion(1) }
                    val companionNameExtensionInvokeDInit by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
                    val companionNameExtensionInvokeEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
                    val companionNameExtensionInvokeFInit by nonInPlaceDelegate { Companion(1) }

                    val classNameExtensionInvokeAInit = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1)
                    val classNameExtensionInvokeBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1) }
                    val classNameExtensionInvokeCInit = nonInPlaceRun { Enum(1) }
                    val classNameExtensionInvokeDInit by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1)
                    val classNameExtensionInvokeEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1) }
                    val classNameExtensionInvokeFInit by nonInPlaceDelegate { Enum(1) }

                    val classAndCompanionNameExtensionInvokeAInit = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
                    val classAndCompanionNameExtensionInvokeBInit = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
                    val classAndCompanionNameExtensionInvokeCInit = nonInPlaceRun { Enum.Companion(1) }
                    val classAndCompanionNameExtensionInvokeDInit by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
                    val classAndCompanionNameExtensionInvokeEInit by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
                    val classAndCompanionNameExtensionInvokeFInit by nonInPlaceDelegate { Enum.Companion(1) }

                    val companionAliasInvokeAInit = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>()
                    val companionAliasInvokeBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>() }
                    val companionAliasInvokeCInit = nonInPlaceRun { EnumCompanionAlias() }
                    val companionAliasInvokeDInit by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>()
                    val companionAliasInvokeEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>() }
                    val companionAliasInvokeFInit by nonInPlaceDelegate { EnumCompanionAlias() }

                    val nestedCompanionAliasInvokeAInit = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>()
                    val nestedCompanionAliasInvokeBInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>() }
                    val nestedCompanionAliasInvokeCInit = nonInPlaceRun { NestedEnumCompanionAlias() }
                    val nestedCompanionAliasInvokeDInit by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>()
                    val nestedCompanionAliasInvokeEInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>() }
                    val nestedCompanionAliasInvokeFInit by nonInPlaceDelegate { NestedEnumCompanionAlias() }
                }

                fun localFun() {
                    val memberA = foo()
                    val memberB = inPlaceRun { foo() }
                    val memberC = nonInPlaceRun { foo() }
                    val memberD by foo()
                    val memberE by inPlaceDelegate { foo() }
                    val memberF by nonInPlaceDelegate { foo() }

                    val companionNameA = Companion.foo()
                    val companionNameB = inPlaceRun { Companion.foo() }
                    val companionNameC = nonInPlaceRun { Companion.foo() }
                    val companionNameD by Companion.foo()
                    val companionNameE by inPlaceDelegate { Companion.foo() }
                    val companionNameF by nonInPlaceDelegate { Companion.foo() }

                    val classNameA = Enum.foo()
                    val classNameB = inPlaceRun { Enum.foo() }
                    val classNameC = nonInPlaceRun { Enum.foo() }
                    val classNameD by Enum.foo()
                    val classNameE by inPlaceDelegate { Enum.foo() }
                    val classNameF by nonInPlaceDelegate { Enum.foo() }

                    val classAndCompanionNameA = Enum.Companion.foo()
                    val classAndCompanionNameB = inPlaceRun { Enum.Companion.foo() }
                    val classAndCompanionNameC = nonInPlaceRun { Enum.Companion.foo() }
                    val classAndCompanionNameD by Enum.Companion.foo()
                    val classAndCompanionNameE by inPlaceDelegate { Enum.Companion.foo() }
                    val classAndCompanionNameF by nonInPlaceDelegate { Enum.Companion.foo() }

                    val companionAliasA = EnumCompanionAlias.foo()
                    val companionAliasB = inPlaceRun { EnumCompanionAlias.foo() }
                    val companionAliasC = nonInPlaceRun { EnumCompanionAlias.foo() }
                    val companionAliasD by EnumCompanionAlias.foo()
                    val companionAliasE by inPlaceDelegate { EnumCompanionAlias.foo() }
                    val companionAliasF by nonInPlaceDelegate { EnumCompanionAlias.foo() }

                    val nestedCompanionAliasA = NestedEnumCompanionAlias.foo()
                    val nestedCompanionAliasB = inPlaceRun { NestedEnumCompanionAlias.foo() }
                    val nestedCompanionAliasC = nonInPlaceRun { NestedEnumCompanionAlias.foo() }
                    val nestedCompanionAliasD by NestedEnumCompanionAlias.foo()
                    val nestedCompanionAliasE by inPlaceDelegate { NestedEnumCompanionAlias.foo() }
                    val nestedCompanionAliasF by nonInPlaceDelegate { NestedEnumCompanionAlias.foo() }

                    val classAliasA = EnumAlias.foo()
                    val classAliasB = inPlaceRun { EnumAlias.foo() }
                    val classAliasC = nonInPlaceRun { EnumAlias.foo() }
                    val classAliasD by EnumAlias.foo()
                    val classAliasE by inPlaceDelegate { EnumAlias.foo() }
                    val classAliasF by nonInPlaceDelegate { EnumAlias.foo() }

                    val nestedClassAliasA = NestedEnumAlias.foo()
                    val nestedClassAliasB = inPlaceRun { NestedEnumAlias.foo() }
                    val nestedClassAliasC = nonInPlaceRun { NestedEnumAlias.foo() }
                    val nestedClassAliasD by NestedEnumAlias.foo()
                    val nestedClassAliasE by inPlaceDelegate { NestedEnumAlias.foo() }
                    val nestedClassAliasF by nonInPlaceDelegate { NestedEnumAlias.foo() }

                    val companionNameInvokeA = Companion()
                    val companionNameInvokeB = inPlaceRun { Companion() }
                    val companionNameInvokeC = nonInPlaceRun { Companion() }
                    val companionNameInvokeD by Companion()
                    val companionNameInvokeE by inPlaceDelegate { Companion() }
                    val companionNameInvokeF by nonInPlaceDelegate { Companion() }

                    val classAndCompanionNameInvokeA = Enum.Companion()
                    val classAndCompanionNameInvokeB = inPlaceRun { Enum.Companion() }
                    val classAndCompanionNameInvokeC = nonInPlaceRun { Enum.Companion() }
                    val classAndCompanionNameInvokeD by Enum.Companion()
                    val classAndCompanionNameInvokeE by inPlaceDelegate { Enum.Companion() }
                    val classAndCompanionNameInvokeF by nonInPlaceDelegate { Enum.Companion() }

                    val companionNameExtensionInvokeA = Companion(1)
                    val companionNameExtensionInvokeB = inPlaceRun { Companion(1) }
                    val companionNameExtensionInvokeC = nonInPlaceRun { Companion(1) }
                    val companionNameExtensionInvokeD by Companion(1)
                    val companionNameExtensionInvokeE by inPlaceDelegate { Companion(1) }
                    val companionNameExtensionInvokeF by nonInPlaceDelegate { Companion(1) }

                    val classNameExtensionInvokeA = Enum(1)
                    val classNameExtensionInvokeB = inPlaceRun { Enum(1) }
                    val classNameExtensionInvokeC = nonInPlaceRun { Enum(1) }
                    val classNameExtensionInvokeD by Enum(1)
                    val classNameExtensionInvokeE by inPlaceDelegate { Enum(1) }
                    val classNameExtensionInvokeF by nonInPlaceDelegate { Enum(1) }

                    val classAndCompanionNameExtensionInvokeA = Enum.Companion(1)
                    val classAndCompanionNameExtensionInvokeB = inPlaceRun { Enum.Companion(1) }
                    val classAndCompanionNameExtensionInvokeC = nonInPlaceRun { Enum.Companion(1) }
                    val classAndCompanionNameExtensionInvokeD by Enum.Companion(1)
                    val classAndCompanionNameExtensionInvokeE by inPlaceDelegate { Enum.Companion(1) }
                    val classAndCompanionNameExtensionInvokeF by nonInPlaceDelegate { Enum.Companion(1) }

                    val companionAliasInvokeA = EnumCompanionAlias()
                    val companionAliasInvokeB = inPlaceRun { EnumCompanionAlias() }
                    val companionAliasInvokeC = nonInPlaceRun { EnumCompanionAlias() }
                    val companionAliasInvokeD by EnumCompanionAlias()
                    val companionAliasInvokeE by inPlaceDelegate { EnumCompanionAlias() }
                    val companionAliasInvokeF by nonInPlaceDelegate { EnumCompanionAlias() }

                    val nestedCompanionAliasInvokeA = NestedEnumCompanionAlias()
                    val nestedCompanionAliasInvokeB = inPlaceRun { NestedEnumCompanionAlias() }
                    val nestedCompanionAliasInvokeC = nonInPlaceRun { NestedEnumCompanionAlias() }
                    val nestedCompanionAliasInvokeD by NestedEnumCompanionAlias()
                    val nestedCompanionAliasInvokeE by inPlaceDelegate { NestedEnumCompanionAlias() }
                    val nestedCompanionAliasInvokeF by nonInPlaceDelegate { NestedEnumCompanionAlias() }
                }
            }
        }
    }
    ;

    val memberA = <!UNINITIALIZED_ENUM_COMPANION!>foo<!>()
    val memberB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>foo<!>() }
    val memberC = nonInPlaceRun { foo() }
    val memberD by <!UNINITIALIZED_ENUM_COMPANION!>foo<!>()
    val memberE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>foo<!>() }
    val memberF by nonInPlaceDelegate { foo() }

    val companionNameA = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
    val companionNameB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
    val companionNameC = nonInPlaceRun { Companion.foo() }
    val companionNameD by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
    val companionNameE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
    val companionNameF by nonInPlaceDelegate { Companion.foo() }

    val classNameA = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo()
    val classNameB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo() }
    val classNameC = nonInPlaceRun { Enum.foo() }
    val classNameD by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo()
    val classNameE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>.foo() }
    val classNameF by nonInPlaceDelegate { Enum.foo() }

    val classAndCompanionNameA = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
    val classAndCompanionNameB = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
    val classAndCompanionNameC = nonInPlaceRun { Enum.Companion.foo() }
    val classAndCompanionNameD by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo()
    val classAndCompanionNameE by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() }
    val classAndCompanionNameF by nonInPlaceDelegate { Enum.Companion.foo() }

    val companionAliasA = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo()
    val companionAliasB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo() }
    val companionAliasC = nonInPlaceRun { EnumCompanionAlias.foo() }
    val companionAliasD by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo()
    val companionAliasE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>.foo() }
    val companionAliasF by nonInPlaceDelegate { EnumCompanionAlias.foo() }

    val nestedCompanionAliasA = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo()
    val nestedCompanionAliasB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo() }
    val nestedCompanionAliasC = nonInPlaceRun { NestedEnumCompanionAlias.foo() }
    val nestedCompanionAliasD by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo()
    val nestedCompanionAliasE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>.foo() }
    val nestedCompanionAliasF by nonInPlaceDelegate { NestedEnumCompanionAlias.foo() }

    val classAliasA = <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo()
    val classAliasB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo() }
    val classAliasC = nonInPlaceRun { EnumAlias.foo() }
    val classAliasD by <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo()
    val classAliasE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumAlias<!>.foo() }
    val classAliasF by nonInPlaceDelegate { EnumAlias.foo() }

    val nestedClassAliasA = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo()
    val nestedClassAliasB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo() }
    val nestedClassAliasC = nonInPlaceRun { NestedEnumAlias.foo() }
    val nestedClassAliasD by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo()
    val nestedClassAliasE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumAlias<!>.foo() }
    val nestedClassAliasF by nonInPlaceDelegate { NestedEnumAlias.foo() }

    val companionNameInvokeA = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
    val companionNameInvokeB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
    val companionNameInvokeC = nonInPlaceRun { Companion() }
    val companionNameInvokeD by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
    val companionNameInvokeE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
    val companionNameInvokeF by nonInPlaceDelegate { Companion() }

    val classAndCompanionNameInvokeA = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
    val classAndCompanionNameInvokeB = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
    val classAndCompanionNameInvokeC = nonInPlaceRun { Enum.Companion() }
    val classAndCompanionNameInvokeD by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>()
    val classAndCompanionNameInvokeE by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() }
    val classAndCompanionNameInvokeF by nonInPlaceDelegate { Enum.Companion() }

    val companionNameExtensionInvokeA = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
    val companionNameExtensionInvokeB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
    val companionNameExtensionInvokeC = nonInPlaceRun { Companion(1) }
    val companionNameExtensionInvokeD by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
    val companionNameExtensionInvokeE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
    val companionNameExtensionInvokeF by nonInPlaceDelegate { Companion(1) }

    val classNameExtensionInvokeA = <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1)
    val classNameExtensionInvokeB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1) }
    val classNameExtensionInvokeC = nonInPlaceRun { Enum(1) }
    val classNameExtensionInvokeD by <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1)
    val classNameExtensionInvokeE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>Enum<!>(1) }
    val classNameExtensionInvokeF by nonInPlaceDelegate { Enum(1) }

    val classAndCompanionNameExtensionInvokeA = Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
    val classAndCompanionNameExtensionInvokeB = inPlaceRun { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
    val classAndCompanionNameExtensionInvokeC = nonInPlaceRun { Enum.Companion(1) }
    val classAndCompanionNameExtensionInvokeD by Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1)
    val classAndCompanionNameExtensionInvokeE by inPlaceDelegate { Enum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) }
    val classAndCompanionNameExtensionInvokeF by nonInPlaceDelegate { Enum.Companion(1) }

    val companionAliasInvokeA = <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>()
    val companionAliasInvokeB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>() }
    val companionAliasInvokeC = nonInPlaceRun { EnumCompanionAlias() }
    val companionAliasInvokeD by <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>()
    val companionAliasInvokeE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>EnumCompanionAlias<!>() }
    val companionAliasInvokeF by nonInPlaceDelegate { EnumCompanionAlias() }

    val nestedCompanionAliasInvokeA = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>()
    val nestedCompanionAliasInvokeB = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>() }
    val nestedCompanionAliasInvokeC = nonInPlaceRun { NestedEnumCompanionAlias() }
    val nestedCompanionAliasInvokeD by <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>()
    val nestedCompanionAliasInvokeE by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumCompanionAlias<!>() }
    val nestedCompanionAliasInvokeF by nonInPlaceDelegate { NestedEnumCompanionAlias() }

    companion object {
        fun foo(): String = "foo()"
        operator fun invoke(): String = "invoke()"
    }
}

enum class EnumWithConstructor(val a: String, val b: String, val c: String) {
    Member(
        a = <!UNINITIALIZED_ENUM_COMPANION!>foo<!>(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>foo<!>() },
        c = nonInPlaceRun { foo() }
    ),
    CompanionName(
        a = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() },
        c = nonInPlaceRun { Companion.foo() }
    ),
    ClassName(
        a = <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructor<!>.foo(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructor<!>.foo() },
        c = nonInPlaceRun { EnumWithConstructor.foo() }
    ),
    ClassAndCompanionName(
        a = EnumWithConstructor.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo(),
        b = inPlaceRun { EnumWithConstructor.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.foo() },
        c = nonInPlaceRun { EnumWithConstructor.Companion.foo() }
    ),
    CompanionAlias(
        a = <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructorCompanionAlias<!>.foo(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructorCompanionAlias<!>.foo() },
        c = nonInPlaceRun { EnumWithConstructorCompanionAlias.foo() }
    ),
    NestedCompanionAlias(
        a = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumWithConstructorCompanionAlias<!>.foo(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumWithConstructorCompanionAlias<!>.foo() },
        c = nonInPlaceRun { NestedEnumWithConstructorCompanionAlias.foo() }
    ),
    ClassAlias(
        a = <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructorAlias<!>.foo(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructorAlias<!>.foo() },
        c = nonInPlaceRun { EnumWithConstructorAlias.foo() }
    ),
    NestedClassAlias(
        a = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumWithConstructorAlias<!>.foo(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumWithConstructorAlias<!>.foo() },
        c = nonInPlaceRun { NestedEnumWithConstructorAlias.foo() }
    ),
    CompanionNameInvoke(
        a = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() },
        c = nonInPlaceRun { Companion() }
    ),
    ClassNameInvoke(
        a = <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructor<!>(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructor<!>() },
        c = nonInPlaceRun { EnumWithConstructor() }
    ),
    ClassAndCompanionNameInvoke(
        a = EnumWithConstructor.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(),
        b = inPlaceRun { EnumWithConstructor.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>() },
        c = nonInPlaceRun { EnumWithConstructor.Companion() }
    ),
    CompanionNameExtensionInvoke(
        a = <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) },
        c = nonInPlaceRun { Companion(1) }
    ),
    ClassNameExtensionInvoke(
        a = <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructor<!>(1),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructor<!>(1) },
        c = nonInPlaceRun { EnumWithConstructor(1) }
    ),
    ClassAndCompanionNameExtensionInvoke(
        a = EnumWithConstructor.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1),
        b = inPlaceRun { EnumWithConstructor.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>(1) },
        c = nonInPlaceRun { EnumWithConstructor.Companion(1) }
    ),
    CompanionAliasInvoke(
        a = <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructorCompanionAlias<!>(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructorCompanionAlias<!>() },
        c = nonInPlaceRun { EnumWithConstructorCompanionAlias() }
    ),
    NestedCompanionAliasInvoke(
        a = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumWithConstructorCompanionAlias<!>(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumWithConstructorCompanionAlias<!>() },
        c = nonInPlaceRun { NestedEnumWithConstructorCompanionAlias() }
    ),
    ClassAliasInvoke(
        a = <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructorAlias<!>(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>EnumWithConstructorAlias<!>() },
        c = nonInPlaceRun { EnumWithConstructorAlias() }
    ),
    NestedClassAliasInvoke(
        a = <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumWithConstructorAlias<!>(),
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>NestedEnumWithConstructorAlias<!>() },
        c = nonInPlaceRun { NestedEnumWithConstructorAlias() }
    );

    companion object {
        fun foo(): String = "foo()"
        operator fun invoke(): String = "invoke()"
    }
}

operator fun Enum.Companion.invoke(x: Int): String = "invoke($x)"
operator fun EnumWithConstructor.Companion.invoke(x: Int): String = "invoke($x)"

typealias EnumCompanionAlias = Enum.Companion
typealias NestedEnumCompanionAlias = EnumCompanionAlias
typealias EnumAlias = Enum
typealias NestedEnumAlias = EnumAlias

typealias EnumWithConstructorCompanionAlias = EnumWithConstructor.Companion
typealias NestedEnumWithConstructorCompanionAlias = EnumWithConstructorCompanionAlias
typealias EnumWithConstructorAlias = EnumWithConstructor
typealias NestedEnumWithConstructorAlias = EnumWithConstructorAlias

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
enumDeclaration, enumEntry, funWithExtensionReceiver, functionDeclaration, functionalType, inline, integerLiteral,
lambdaLiteral, localProperty, nullableType, objectDeclaration, operator, primaryConstructor, propertyDeclaration,
propertyDelegate, starProjection, stringLiteral, thisExpression, typeAliasDeclaration, typeParameter */
