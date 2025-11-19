// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82466

interface Base
class Derived: Base

class C(val k: Derived): Base by return k
