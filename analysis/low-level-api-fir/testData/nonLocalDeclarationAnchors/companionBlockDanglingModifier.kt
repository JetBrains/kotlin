// LANGUAGE: +CompanionBlocksAndExtensions

class Owner {
    companion {
        fun before() {}

        for (i in 1..10) {
        }
    }

    companion {
        fun t2() {}

        @Anno
    }

    @Anno2
    companion {
        fun t3() {}

        @Anno3
    }

    @Anno4
}
