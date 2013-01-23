fun box() : String {
    System.out?.println(System.out?.println(10.toFloat()..11.toFloat()))

    for(f in 10.toFloat()..11.toFloat() step 0.3.toFloat()) {
        System.out?.println(f)
    }

    for(f in 10.toDouble()..11.toDouble() step 0.3.toDouble()) {
        System.out?.println(f)
    }

    return "OK"
}
