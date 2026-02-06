import { ping, Something } from "./recursiveExport_v5.mjs"

export default function() {
    return {
        "pingCall": function() {
            return ping(new Something())
        },
    };
};