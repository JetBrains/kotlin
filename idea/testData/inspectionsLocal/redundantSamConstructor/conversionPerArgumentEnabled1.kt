// COMPILER_ARGUMENTS: -XXLanguage:+NewInference -XXLanguage:+SamConversionForKotlinFunctions -XXLanguage:+FunctionalInterfaceConversion -XXLanguage:+SamConversionPerArgument

fun usage(r1: Runnable) {
    Test.test(r1, Runnable<caret> {})
}
