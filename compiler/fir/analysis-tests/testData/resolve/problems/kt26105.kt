// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-26105
// WITH_STDLIB

// KT-26105: "Name shadowed" warning doesn't work for nested lambdas
fun f(name: String) {
    listOf<String>().forEach { name ->
        println(name)
    }
}

fun g(name: String) {
    listOf<List<String>>().forEach { list ->
        list.forEach { name ->
            println(name)
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */