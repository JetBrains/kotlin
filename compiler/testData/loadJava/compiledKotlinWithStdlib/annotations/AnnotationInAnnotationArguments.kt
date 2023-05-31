// PLATFORM_DEPENDANT_METADATA
// ALLOW_AST_ACCESS
package test

enum class E { ENTRY }

annotation class StringOptions(vararg val option: String)
annotation class EnumOption(val option: E)

annotation class OptionGroups(val o1: StringOptions, val o2: EnumOption)

@OptionGroups(StringOptions("abc", "d", "ef"), EnumOption(E.ENTRY))
public class AnnotationInAnnotationArguments
