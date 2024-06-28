fun compute(e: E): String = when (e) {
    E.UNCHANGED1 -> "OK"
    E.REMOVED -> "FAIL1"
    E.UNCHANGED2 -> "FAIL2"
}
