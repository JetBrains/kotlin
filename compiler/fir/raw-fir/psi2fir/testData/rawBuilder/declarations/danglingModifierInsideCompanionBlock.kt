// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    companion {
        fun t() {
        }
        for (a in 1..10) {

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
