@file:DependsOn("@{runtime}")

fun main() {
    error("my error")
}

fun a() {
    main()
}

a()