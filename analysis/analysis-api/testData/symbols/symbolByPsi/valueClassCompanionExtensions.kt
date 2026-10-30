// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
// TARGET_PLATFORM: JVM

@JvmInline
value class ValueClass(val value: String)

companion fun ValueClass.companionExtensionFunction() {}
companion fun ValueClass.companionExtensionFunctionWithValueClassParameter(v: ValueClass) {}

companion val ValueClass.companionExtensionProperty: Int get() = 0
companion var ValueClass.companionExtensionVariable: Int
    get() = 0
    set(value) {}
