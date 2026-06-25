// ISSUE: KT-87226
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

object SingletonObject
typealias AliasToObject = SingletonObject

companion fun <!COMPANION_EXTENSION_RECEIVER_IS_OBJECT!>AliasToObject<!>.objectViaAlias() {}
companion fun <!COMPANION_EXTENSION_RECEIVER_IS_OBJECT!>SingletonObject<!>.objectDirectly() {}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, objectDeclaration, typeAliasDeclaration */
