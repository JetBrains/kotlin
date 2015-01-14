fun box(): String {
    var bottles = 99
    while (bottles > 0) {
        // System.out.println("bottles of beer on the wall");
        bottles -= 1
        bottles--
    }
    return if (bottles == -1) "OK" else "Fail $bottles"
}
