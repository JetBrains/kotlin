fun box() : String {
    System.out?.println(System.out?.println(10.flt..11.flt))

    for(f in 10.flt..11.flt step 0.3.flt) {
        System.out?.println(f)
    }

    for(f in 10.dbl..11.dbl step 0.3.dbl) {
        System.out?.println(f)
    }

    return "OK"
}