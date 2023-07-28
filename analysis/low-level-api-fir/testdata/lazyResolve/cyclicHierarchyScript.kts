interface B : A, ResolveMe {}
interface C : B {}
interface D : B {}
interface ResolveMe<caret> : F {}
interface F : D, C {}
interface NonLoopedInterface : C