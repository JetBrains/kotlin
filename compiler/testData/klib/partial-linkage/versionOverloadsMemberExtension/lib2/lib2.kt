fun computeMemberExt(): String {
    val h = C()
    val s = with(h) { "test".ex() }
    return if (s == "s=test,b=p,c=4") "OK" else "FAIL"
}

