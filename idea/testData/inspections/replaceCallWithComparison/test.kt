fun foo() {
    1.equals(1)         // YES
    !1.equals(2)        // YES
    1.compareTo(1)      // NO
    1.compareTo(1) == 0 // NO
    2.compareTo(1) > 0  // YES
    0 >= 1.compareTo(2) // YES
    2.plus(2)           // NO
    2.times(2)          // NO
}