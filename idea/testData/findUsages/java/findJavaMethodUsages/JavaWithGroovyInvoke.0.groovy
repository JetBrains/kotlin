class GroovyClass extends JavaWithGroovyInvoke_0 {
    def fieldNoType = new JavaWithGroovyInvoke_0.OtherJavaClass()
    def JavaWithGroovyInvoke_0.OtherJavaClass fieldWithType = new JavaWithGroovyInvoke_0.OtherJavaClass()

    def methodNoType() {
        new JavaWithGroovyInvoke_0.OtherJavaClass()
    }

    JavaWithGroovyInvoke_0.OtherJavaClass methodWithType() {
        new JavaWithGroovyInvoke_0.OtherJavaClass()
    }
}