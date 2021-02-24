
fun f() : Result<Int> = Result.success(42)

println(f().getOrNull())
