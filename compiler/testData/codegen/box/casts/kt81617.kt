fun foo(o: Any): Int {
    do {
        do {
            do {
                do {
                    do {
                        do {
                            do {
                                do {
                                    do {
                                        do {
                                            do {
                                                do {
                                                    do {
                                                        do {
                                                            do {
                                                                do {
                                                                    do {
                                                                        do {
                                                                            do {
                                                                                do {
                                                                                    do {
                                                                                        do {
                                                                                            do {
                                                                                                do {
                                                                                                    do {
                                                                                                        do {
                                                                                                            do {
                                                                                                                do {
                                                                                                                    do {
                                                                                                                        do {
                                                                                                                            if (o is String) return o.length
                                                                                                                        } while (false)
                                                                                                                    } while (false)
                                                                                                                } while (false)
                                                                                                            } while (false)
                                                                                                        } while (false)
                                                                                                    } while (false)
                                                                                                } while (false)
                                                                                            } while (false)
                                                                                        } while (false)
                                                                                    } while (false)
                                                                                } while (false)
                                                                            } while (false)
                                                                        } while (false)
                                                                    } while (false)
                                                                } while (false)
                                                            } while (false)
                                                        } while (false)
                                                    } while (false)
                                                } while (false)
                                            } while (false)
                                        } while (false)
                                    } while (false)
                                } while (false)
                            } while (false)
                        } while (false)
                    } while (false)
                } while (false)
            } while (false)
        } while (false)
    } while (false)

    return 42
}

fun box(): String {
    val result = foo("zzz")
    return if (result == 3) "OK" else "fail: $result"
}
