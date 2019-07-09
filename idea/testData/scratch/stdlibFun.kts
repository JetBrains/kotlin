// REPL_MODE: ~REPL_MODE~

arrayListOf(1, 5, 7).map { it * 2 }
    .filter { it < 10 }
    .find { it == 2 }