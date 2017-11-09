package test

fun useListImpl() = object : ListImpl() {
                        override fun func() = 42
                    }.func()
