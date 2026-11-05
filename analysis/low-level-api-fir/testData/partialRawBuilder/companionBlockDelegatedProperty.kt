// PROPERTY: foo
// LANGUAGE: +CompanionBlocks +CompanionExtensions

package test.classes

class Owner {
    companion {
        var foo: Int by Delegate()
    }
}
