fun A.some(s: String) {

}

class A {
    private fun some(s: String) {

    }

    fun usage() {
        // private some shadows extension
        some("lol"<caret>)
    }
}


//Text: (<highlight>s: String</highlight>), Disabled: false, Strikeout: false, Green: true