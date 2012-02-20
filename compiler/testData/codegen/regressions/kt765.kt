fun box() : String {
    System.out?.println(System.out?.println(10.float..11.float))

    for(f in 10.float..11.float step 0.3.float) {
        System.out?.println(f)
    }

    for(f in 10.double..11.double step 0.3.double) {
        System.out?.println(f)
    }

    return "OK"
}