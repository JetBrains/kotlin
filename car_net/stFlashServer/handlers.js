/**
 * Created by user on 7/11/16.
 */
const fs = require("fs");
const main = require("./main.js");

function loadBin(httpContent, response) {

    var uploadClass = main.protoConstructor.Upload;
    var uploadObject = uploadClass.decode(httpContent);
    fs.writeFile(binFilePath, uploadObject.data.buffer, "binary", function (error) {
        var uploadResultClass = main.protoConstructor.UploadResult;
        var code = 0;
        var stdErr = "";
        if (error) {
            code = 2;
            stdErr = error.toString();
        } else {
            code = 0;
            console.log(main.commandPrefix + " " + main.binFilePath + " " + uploadObject.base);
        }
        var resultObject = new uploadResultClass({
            "stdOut": "",
            "resultCode": code,
            "stdErr": stdErr
        });
        var byteBuffer = resultObject.encode();
        var byteArray = [];
        for (var i = 0; i < byteBuffer.limit; i++) {
            byteArray.push(byteBuffer.buffer[i]);
        }
        response.writeHead(200, {"Content-Type": "text/plain", "Content-length": byteArray.length});
        response.write(new Buffer(byteArray));
        response.end();
    });
}

function other(httpContent, response) {
    console.log("other");
    response.writeHead(200, {"Content-Type": "text/plain"});
    response.end();
}


exports.loadBin = loadBin;
exports.other = other;