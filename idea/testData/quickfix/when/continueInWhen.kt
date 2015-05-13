// "Add label to loop" "true"
fun breakContinueInWhen(i: Int) {
    for (y in 0..10) {
        when(i) {
            0 -> co<caret>ntinue
            2 -> {
                for(z in 0..10) {
                    break
                }
                for(w in 0..10) {
                    continue
                }
            }
        }
    }
}
