package test

public inline fun <reified T : Any> inlineMeIfYouCan(): String? =
        {
            f {
                T::class.java.getName()
            }
        }()

inline fun f(x: () -> String) = x()