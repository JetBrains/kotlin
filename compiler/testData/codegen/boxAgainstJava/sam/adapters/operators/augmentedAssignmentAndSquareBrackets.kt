fun box(): String {
    var c = Container()
    var indexAccess = 0

    // TODO uncomment when KT-3723 is fixed

    //var v1 = "FAIL"
    //c[{ indexAccess++ }] += { v1 = "OK" }
    //if (v1 != "OK") return "plus: $v1"
    //
    //var v2 = "FAIL"
    //c[{ indexAccess++ }] -= { v2 = "OK" }
    //if (v2 != "OK") return "minus: $v2"
    //
    //var v3 = "FAIL"
    //c[{ indexAccess++ }] *= { v3 = "OK" }
    //if (v3 != "OK") return "times: $v3"
    //
    //var v4 = "FAIL"
    //c[{ indexAccess++ }] /= { v4 = "OK" }
    //if (v4 != "OK") return "div: $v4"
    //
    //var v5 = "FAIL"
    //c[{ indexAccess++ }] %= { v5 = "OK" }
    //if (v5 != "OK") return "mod: $v5"
    //
    //if (indexAccess != 10) return indexAccess

    return "OK"
}
