package foo

class Impl: Foo() {
    <caret>
}

// KT-4732 Override/Implement action does not add all imports when "Optimize imports on the fly" is enabled
