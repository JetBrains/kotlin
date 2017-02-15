fun box() =
        if (getAndCheck({ 42 }, { 42 })) "OK" else "fail"

inline fun <T> getAndCheck(getFirst: () -> T, getSecond: () -> T) =
        getFirst() == getSecond()