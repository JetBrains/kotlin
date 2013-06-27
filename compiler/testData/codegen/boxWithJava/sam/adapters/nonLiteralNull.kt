fun box(): String {
    val f: (() -> Unit)? = null
    return JavaClass.run(f)!!
}