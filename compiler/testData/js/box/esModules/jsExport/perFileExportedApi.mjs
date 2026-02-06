import { create } from "./perFileExportedApi-kotlin_main_v5/function.export.mjs";

export default function() {
    let result = ""
    const a = create()

    result += a.bar
    a.bar = "not bar"
    result += `&${a.bar}`
    result += `&${a.foo()}`
    result += `&${a.ping()}`
    result += `&${a.pong}`

    return result
};
