package returnPack

inline fun foo1(action: (Int) -> Unit) {

}

inline fun foo2(action: (String) -> Unit) {

}

inline fun foo3(action: (Boolean) -> Unit) {

}

fun usage() {
    foo1 {
        foo2 {
            foo3 {
                foo1(
                    {
                        return@foo3
                        return@foo2
                        return@foo1
                    },
                )

                foo2(label@ fun(s: String) {
                    return
                    return@label
                    return@foo3
                    return@foo2
                    return@foo1
                })

                return@foo3
                return@foo2
                return@foo1
            }
        }
    }

    return
}

