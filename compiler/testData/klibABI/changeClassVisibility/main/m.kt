import abitestutils.abiTest
import abitestutils.TestBuilder

fun box() = abiTest {
    success("PublicTopLevelClass") { PublicTopLevelClass_valueParameter(null) }
    success("PublicTopLevelClass") { PublicTopLevelClass_returnType().toString() }
    success("PublicTopLevelClass") { PublicTopLevelClass_anyReturnType().toString() }
    // TODO: KT-54469, accessing PublicTopLevelClass.PublicToInternalNestedClass should fail.
    success("PublicTopLevelClass.PublicToInternalNestedClass") { PublicTopLevelClass_PublicToInternalNestedClass_valueParameter(null) }
    success("PublicTopLevelClass.PublicToInternalNestedClass") { PublicTopLevelClass_PublicToInternalNestedClass_returnType().toString() }
    success("PublicTopLevelClass.PublicToInternalNestedClass") { PublicTopLevelClass_PublicToInternalNestedClass_anyReturnType().toString() }
    success("PublicTopLevelClass.PublicToProtectedNestedClass") { PublicTopLevelClass_PublicToProtectedNestedClass_valueParameter(null) }
    success("PublicTopLevelClass.PublicToProtectedNestedClass") { PublicTopLevelClass_PublicToProtectedNestedClass_returnType().toString() }
    success("PublicTopLevelClass.PublicToProtectedNestedClass") { PublicTopLevelClass_PublicToProtectedNestedClass_anyReturnType().toString() }
    // TODO: KT-54469, accessing PublicTopLevelClass.PublicToPrivateNestedClass should fail.
    success("PublicTopLevelClass.PublicToPrivateNestedClass") { PublicTopLevelClass_PublicToPrivateNestedClass_valueParameter(null) }
    success("PublicTopLevelClass.PublicToPrivateNestedClass") { PublicTopLevelClass_PublicToPrivateNestedClass_returnType().toString() }
    success("PublicTopLevelClass.PublicToPrivateNestedClass") { PublicTopLevelClass_PublicToPrivateNestedClass_anyReturnType().toString() }
    // TODO: KT-54469, accessing PublicTopLevelClass.PublicToInternalInnerClass should fail.
    success("PublicTopLevelClass.PublicToInternalInnerClass") { PublicTopLevelClass_PublicToInternalInnerClass_valueParameter(null) }
    success("PublicTopLevelClass.PublicToInternalInnerClass") { PublicTopLevelClass_PublicToInternalInnerClass_returnType().toString() }
    success("PublicTopLevelClass.PublicToInternalInnerClass") { PublicTopLevelClass_PublicToInternalInnerClass_anyReturnType().toString() }
    success("PublicTopLevelClass.PublicToProtectedInnerClass") { PublicTopLevelClass_PublicToProtectedInnerClass_valueParameter(null) }
    success("PublicTopLevelClass.PublicToProtectedInnerClass") { PublicTopLevelClass_PublicToProtectedInnerClass_returnType().toString() }
    success("PublicTopLevelClass.PublicToProtectedInnerClass") { PublicTopLevelClass_PublicToProtectedInnerClass_anyReturnType().toString() }
    // TODO: KT-54469, accessing PublicTopLevelClass.PublicToPrivateInnerClass should fail.
    success("PublicTopLevelClass.PublicToPrivateInnerClass") { PublicTopLevelClass_PublicToPrivateInnerClass_valueParameter(null) }
    success("PublicTopLevelClass.PublicToPrivateInnerClass") { PublicTopLevelClass_PublicToPrivateInnerClass_returnType().toString() }
    success("PublicTopLevelClass.PublicToPrivateInnerClass") { PublicTopLevelClass_PublicToPrivateInnerClass_anyReturnType().toString() }

    // TODO: KT-54469, accessing PublicToInternalTopLevelClass and all nested classes should fail.
    success("PublicToInternalTopLevelClass") { PublicToInternalTopLevelClass_valueParameter(null) }
    success("PublicToInternalTopLevelClass") { PublicToInternalTopLevelClass_returnType().toString() }
    success("PublicToInternalTopLevelClass") { PublicToInternalTopLevelClass_anyReturnType().toString() }
    success("PublicToInternalTopLevelClass.PublicToInternalNestedClass") { PublicToInternalTopLevelClass_PublicToInternalNestedClass_valueParameter(null) }
    success("PublicToInternalTopLevelClass.PublicToInternalNestedClass") { PublicToInternalTopLevelClass_PublicToInternalNestedClass_returnType().toString() }
    success("PublicToInternalTopLevelClass.PublicToInternalNestedClass") { PublicToInternalTopLevelClass_PublicToInternalNestedClass_anyReturnType().toString() }
    success("PublicToInternalTopLevelClass.PublicToProtectedNestedClass") { PublicToInternalTopLevelClass_PublicToProtectedNestedClass_valueParameter(null) }
    success("PublicToInternalTopLevelClass.PublicToProtectedNestedClass") { PublicToInternalTopLevelClass_PublicToProtectedNestedClass_returnType().toString() }
    success("PublicToInternalTopLevelClass.PublicToProtectedNestedClass") { PublicToInternalTopLevelClass_PublicToProtectedNestedClass_anyReturnType().toString() }
    success("PublicToInternalTopLevelClass.PublicToPrivateNestedClass") { PublicToInternalTopLevelClass_PublicToPrivateNestedClass_valueParameter(null) }
    success("PublicToInternalTopLevelClass.PublicToPrivateNestedClass") { PublicToInternalTopLevelClass_PublicToPrivateNestedClass_returnType().toString() }
    success("PublicToInternalTopLevelClass.PublicToPrivateNestedClass") { PublicToInternalTopLevelClass_PublicToPrivateNestedClass_anyReturnType().toString() }
    success("PublicToInternalTopLevelClass.PublicToInternalInnerClass") { PublicToInternalTopLevelClass_PublicToInternalInnerClass_valueParameter(null) }
    success("PublicToInternalTopLevelClass.PublicToInternalInnerClass") { PublicToInternalTopLevelClass_PublicToInternalInnerClass_returnType().toString() }
    success("PublicToInternalTopLevelClass.PublicToInternalInnerClass") { PublicToInternalTopLevelClass_PublicToInternalInnerClass_anyReturnType().toString() }
    success("PublicToInternalTopLevelClass.PublicToProtectedInnerClass") { PublicToInternalTopLevelClass_PublicToProtectedInnerClass_valueParameter(null) }
    success("PublicToInternalTopLevelClass.PublicToProtectedInnerClass") { PublicToInternalTopLevelClass_PublicToProtectedInnerClass_returnType().toString() }
    success("PublicToInternalTopLevelClass.PublicToProtectedInnerClass") { PublicToInternalTopLevelClass_PublicToProtectedInnerClass_anyReturnType().toString() }
    success("PublicToInternalTopLevelClass.PublicToPrivateInnerClass") { PublicToInternalTopLevelClass_PublicToPrivateInnerClass_valueParameter(null) }
    success("PublicToInternalTopLevelClass.PublicToPrivateInnerClass") { PublicToInternalTopLevelClass_PublicToPrivateInnerClass_returnType().toString() }
    success("PublicToInternalTopLevelClass.PublicToPrivateInnerClass") { PublicToInternalTopLevelClass_PublicToPrivateInnerClass_anyReturnType().toString() }

    unlinkedSymbolInValueParameter("/PublicToPrivateTopLevelClass") { PublicToPrivateTopLevelClass_valueParameter(null) }
    unlinkedSymbolInReturnType("/PublicToPrivateTopLevelClass") { PublicToPrivateTopLevelClass_returnType().toString() }
    unlinkedConstructorSymbol("/PublicToPrivateTopLevelClass.<init>") { PublicToPrivateTopLevelClass_anyReturnType().toString() }
    unlinkedSymbolInValueParameter("/PublicToPrivateTopLevelClass.PublicToInternalNestedClass") { PublicToPrivateTopLevelClass_PublicToInternalNestedClass_valueParameter(null) }
    unlinkedSymbolInReturnType("/PublicToPrivateTopLevelClass.PublicToInternalNestedClass") { PublicToPrivateTopLevelClass_PublicToInternalNestedClass_returnType().toString() }
    unlinkedConstructorSymbol("/PublicToPrivateTopLevelClass.PublicToInternalNestedClass.<init>") { PublicToPrivateTopLevelClass_PublicToInternalNestedClass_anyReturnType().toString() }
    unlinkedSymbolInValueParameter("/PublicToPrivateTopLevelClass.PublicToProtectedNestedClass") { PublicToPrivateTopLevelClass_PublicToProtectedNestedClass_valueParameter(null) }
    unlinkedSymbolInReturnType("/PublicToPrivateTopLevelClass.PublicToProtectedNestedClass") { PublicToPrivateTopLevelClass_PublicToProtectedNestedClass_returnType().toString() }
    unlinkedConstructorSymbol("/PublicToPrivateTopLevelClass.PublicToProtectedNestedClass.<init>") { PublicToPrivateTopLevelClass_PublicToProtectedNestedClass_anyReturnType().toString() }
    unlinkedSymbolInValueParameter("/PublicToPrivateTopLevelClass.PublicToPrivateNestedClass") { PublicToPrivateTopLevelClass_PublicToPrivateNestedClass_valueParameter(null) }
    unlinkedSymbolInReturnType("/PublicToPrivateTopLevelClass.PublicToPrivateNestedClass") { PublicToPrivateTopLevelClass_PublicToPrivateNestedClass_returnType().toString() }
    unlinkedConstructorSymbol("/PublicToPrivateTopLevelClass.PublicToPrivateNestedClass.<init>") { PublicToPrivateTopLevelClass_PublicToPrivateNestedClass_anyReturnType().toString() }
    unlinkedSymbolInValueParameter("/PublicToPrivateTopLevelClass.PublicToInternalInnerClass") { PublicToPrivateTopLevelClass_PublicToInternalInnerClass_valueParameter(null) }
    unlinkedSymbolInReturnType("/PublicToPrivateTopLevelClass.PublicToInternalInnerClass") { PublicToPrivateTopLevelClass_PublicToInternalInnerClass_returnType().toString() }
    unlinkedConstructorSymbol("/PublicToPrivateTopLevelClass.<init>") { PublicToPrivateTopLevelClass_PublicToInternalInnerClass_anyReturnType().toString() }
    unlinkedSymbolInValueParameter("/PublicToPrivateTopLevelClass.PublicToProtectedInnerClass") { PublicToPrivateTopLevelClass_PublicToProtectedInnerClass_valueParameter(null) }
    unlinkedSymbolInReturnType("/PublicToPrivateTopLevelClass.PublicToProtectedInnerClass") { PublicToPrivateTopLevelClass_PublicToProtectedInnerClass_returnType().toString() }
    unlinkedConstructorSymbol("/PublicToPrivateTopLevelClass.<init>") { PublicToPrivateTopLevelClass_PublicToProtectedInnerClass_anyReturnType().toString() }
    unlinkedSymbolInValueParameter("/PublicToPrivateTopLevelClass.PublicToPrivateInnerClass") { PublicToPrivateTopLevelClass_PublicToPrivateInnerClass_valueParameter(null) }
    unlinkedSymbolInReturnType("/PublicToPrivateTopLevelClass.PublicToPrivateInnerClass") { PublicToPrivateTopLevelClass_PublicToPrivateInnerClass_returnType().toString() }
    unlinkedConstructorSymbol("/PublicToPrivateTopLevelClass.<init>") { PublicToPrivateTopLevelClass_PublicToPrivateInnerClass_anyReturnType().toString() }

    success("PublicTopLevelClassInheritor") { PublicTopLevelClassInheritor().toString() }
    // TODO: KT-54469, creating instance of PublicToInternalTopLevelClassInheritor should fail.
    success("PublicToInternalTopLevelClassInheritor") { PublicToInternalTopLevelClassInheritor().toString() }
    expectFailure(linkage("Constructor PublicToPrivateTopLevelClassInheritor.<init> can not be called: Constructor PublicToPrivateTopLevelClassInheritor.<init> uses unlinked class symbol /PublicToPrivateTopLevelClass (through class PublicToPrivateTopLevelClassInheritor)")) { PublicToPrivateTopLevelClassInheritor() }
}

// Shortcuts:
private inline fun TestBuilder.success(expectedOutcome: String, noinline block: () -> String) =
    expectSuccess(expectedOutcome, block)

private inline fun TestBuilder.unlinkedSymbolInValueParameter(signature: String, noinline block: () -> Unit) {
    val functionName = signature.removePrefix("/").replace('.', '_') + "_valueParameter"
    unlinkedSymbol(signature, functionName, block)
}

private inline fun TestBuilder.unlinkedSymbolInReturnType(signature: String, noinline block: () -> Unit) {
    val functionName = signature.removePrefix("/").replace('.', '_') + "_returnType"
    unlinkedSymbol(signature, functionName, block)
}

private inline fun TestBuilder.unlinkedConstructorSymbol(signature: String, noinline block: () -> Unit) {
    val constructorName = signature.removePrefix("/").split('.').takeLast(2).joinToString(".")
    expectFailure(linkage("Constructor $constructorName can not be called: No constructor found for symbol $signature"), block)
}

private inline fun TestBuilder.unlinkedSymbol(signature: String, functionName: String, noinline block: () -> Unit) {
    expectFailure(linkage("Function $functionName can not be called: Function $functionName uses unlinked class symbol $signature"), block)
}
