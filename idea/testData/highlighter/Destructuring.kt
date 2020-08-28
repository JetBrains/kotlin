// IGNORE_FIR
// EXPECTED_DUPLICATED_HIGHLIGHTING

<info descr="null">data</info> class <info descr="null">Box</info>(val <info descr="null">v</info>: <info descr="null">Int</info>)
fun <info descr="null">consume</info>(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used"><info descr="null">x</info></warning>: <info descr="null">Int</info>) {}

fun <info descr="null">some</info>() {
    val (<info textAttributesKey="KOTLIN_LOCAL_VARIABLE">s</info>) = <info descr="null">Box</info>(0)
    var (<info textAttributesKey="KOTLIN_LOCAL_VARIABLE"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">x</info></info>) = <info descr="null">Box</info>(1)

    <info descr="null">consume</info>(<info descr="null">s</info>)
    <info descr="null">consume</info>(<info textAttributesKey="KOTLIN_LOCAL_VARIABLE"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">x</info></info>)

    <info textAttributesKey="KOTLIN_LOCAL_VARIABLE"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">x</info></info> = <info textAttributesKey="KOTLIN_LOCAL_VARIABLE"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">x</info></info> * 2 + 2
    <info descr="null">consume</info>(<info textAttributesKey="KOTLIN_LOCAL_VARIABLE"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">x</info></info>)
}