fun test() {
    val empty = "${}"
    val s0 = "${$}"
    val s1 = "${42}"
    val s2 = "${42/**/}"
    val s3 = "${42/**/ /**/}"
    val s4 = "${42;}"
    val s5 = "${42$}"
}