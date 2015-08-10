// TARGET_CLASS: X

// INFO: {"checked": "true"}
interface X

// INFO: {"checked": "false"}
interface Y

// INFO: {"checked": "true"}
interface Z

class <caret>B: X, Y, Z