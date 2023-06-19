listOf(1, 2, 3, 4, 5)
    .filter { (it % 2) == 0 }
    .map { it * 2 }
    .forEach { System.out.println(it) }