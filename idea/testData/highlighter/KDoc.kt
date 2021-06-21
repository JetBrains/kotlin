// IGNORE_FIR

/**
 * @param <info descr="null" textAttributesKey="KDOC_LINK">x</info> foo and <info descr="null" textAttributesKey="KDOC_LINK">[baz]</info>
 * @param <info descr="null" textAttributesKey="KDOC_LINK">y</info> bar
 * @return notALink here
 */
fun <info descr="null">f</info>(<info descr="null">x</info>: <info descr="null">Int</info>, <info descr="null">y</info>: <info descr="null">Int</info>): <info descr="null">Int</info> {
return <info descr="null">x</info> + <info descr="null">y</info>
}

fun <info descr="null">baz</info>() {}