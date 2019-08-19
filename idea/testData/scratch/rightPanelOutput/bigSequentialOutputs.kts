// PREVIEW_ENABLED: TRUE
println("Hello world")

for (x in 1..1) {
    println("Hello world")
}

for (x in 1..3) {
    println("Hello world")
}

for (x in 1..4) {
    println("Hello world")
}

println(List(3) { "Hello world" }.joinToString(separator = "\n"))

println(
    List(3) { "Hello world" }.joinToString(separator = "\n")
)