package extensionMemberFunction

fun main(args: Array<String>) {
    MemberClass().testMember(ExtClass())
    MemberClass.testCompanion(ExtClass())
}

class MemberClass {
    fun testMember(extClass: ExtClass) {
        // EXPRESSION: extClass.testPublic()
        // RESULT: 1: I
        //Breakpoint!
        extClass.testPublic()

        with(extClass) {
            // EXPRESSION: testPublic()
            // RESULT: Following declarations are unavailable in debug scope: testPublic
            //Breakpoint!
            testPublic()
        }

        // EXPRESSION: extClass.testPrivate()
        // RESULT: 1: I
        //Breakpoint!
        extClass.testPrivate()

        with(extClass) {
            // EXPRESSION: testPrivate()
            // RESULT: Following declarations are unavailable in debug scope: testPrivate
            //Breakpoint!
            testPrivate()
        }

        extClass.testExtMember()
    }

    fun ExtClass.testExtMember() {
        // EXPRESSION: testPublic()
        // RESULT: Following declarations are unavailable in debug scope: testPublic
        //Breakpoint!
        testPublic()

        // EXPRESSION: this.testPublic()
        // RESULT: A receiver of type extensionMemberFunction.ExtClass is required
        //Breakpoint!
        this.testPublic()

        // EXPRESSION: testPrivate()
        // RESULT: Following declarations are unavailable in debug scope: testPrivate
        //Breakpoint!
        testPrivate()

        // EXPRESSION: this.testPrivate()
        // RESULT: A receiver of type extensionMemberFunction.ExtClass is required
        //Breakpoint!
        this.testPrivate()
    }

    public fun ExtClass.testPublic() = a
    private fun ExtClass.testPrivate() = a

    companion object {
        public fun ExtClass.testCompPublic() = a
        private fun ExtClass.testCompPrivate() = a

        fun testCompanion(extClass: ExtClass) {
            // EXPRESSION: extClass.testCompPublic()
            // RESULT: 1: I
            //Breakpoint!
            extClass.testCompPublic()

            with(extClass) {
                // EXPRESSION: testCompPublic()
                // RESULT: Following declarations are unavailable in debug scope: testCompPublic
                //Breakpoint!
                testCompPublic()
            }

            // EXPRESSION: extClass.testCompPrivate()
            // RESULT: 1: I
            //Breakpoint!
            extClass.testCompPrivate()

            with(extClass) {
                // EXPRESSION: testCompPrivate()
                // RESULT: Following declarations are unavailable in debug scope: testCompPrivate
                //Breakpoint!
                testCompPrivate()
            }

            extClass.testExtCompanion()
        }

        fun ExtClass.testExtCompanion() {
            // EXPRESSION: testCompPublic()
            // RESULT: Following declarations are unavailable in debug scope: testCompPublic
            //Breakpoint!
            testCompPublic()

            // EXPRESSION: this.testCompPublic()
            // RESULT: A receiver of type extensionMemberFunction.ExtClass is required
            //Breakpoint!
            this.testCompPublic()

            // EXPRESSION: testCompPrivate()
            // RESULT: Following declarations are unavailable in debug scope: testCompPrivate
            //Breakpoint!
            testCompPrivate()

            // EXPRESSION: this.testCompPrivate()
            // RESULT: A receiver of type extensionMemberFunction.ExtClass is required
            //Breakpoint!
            this.testCompPrivate()
        }
    }
}

class ExtClass {
    val a = 1
}