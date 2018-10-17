fun foo(map: Map<String, String>) {
    map.forEach { (t, u) ->
        println("$t: $u")
    }
}