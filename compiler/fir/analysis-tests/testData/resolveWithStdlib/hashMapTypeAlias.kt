// RUN_PIPELINE_TILL: BACKEND
typealias MyT<X> = HashMap<X, Int>

fun <X> MyT<X>.add(x: X, y: Int) {}

fun main() {
    MyT<String>().apply {
        add("1", 2)
    }
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, integerLiteral, lambdaLiteral, nullableType,
stringLiteral, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
