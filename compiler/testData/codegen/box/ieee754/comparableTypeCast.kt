fun box(): String {
    if ((-0.0 as Comparable<Double>) >= 0.0) return "fail 0"
    if ((-0.0F as Comparable<Float>) >= 0.0F) return "fail 1"


    if ((-0.0 as Comparable<Double>) == 0.0) return "fail 3"
    if (-0.0 == (0.0 as Comparable<Double>)) return "fail 4"

    if ((-0.0F as Comparable<Float>) == 0.0F) return "fail 5"
    if (-0.0F == (0.0F as Comparable<Float>)) return "fail 6"

    return "OK"
}