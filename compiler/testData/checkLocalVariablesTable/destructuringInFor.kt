// WITH_RUNTIME

fun box() {
    val map: Map<String, String> = mapOf()
    for ((a, b) in map) {
        a + b
    }
}

// METHOD : DestructuringInForKt.box()V

// VARIABLE : NAME=b TYPE=Ljava/lang/String; INDEX=4
// VARIABLE : NAME=a TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=map TYPE=Ljava/util/Map; INDEX=0