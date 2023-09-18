interface B : A, ResolveMe {}
interface C1 : B {}
interface C2 : C1 {}
interface D1 : B {}
interface D2 : D1 {}
interface ResolveMe<caret> : F {}
interface F : D2, C2 {}
interface NonLoopedInterface : C2