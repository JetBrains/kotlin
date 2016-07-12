/**
 * Created by user on 7/11/16.
 */
const fs = require("fs");
const main = require("./main.js");
var exec = require('child_process').exec;

function loadBin(httpContent, response) {

    var uploadClass = main.protoConstructor.Upload;
    var uploadObject = uploadClass.decode(httpContent);
    fs.writeFile(main.binFilePath, uploadObject.data.buffer, "binary", function (error) {
        var uploadResultClass = main.protoConstructor.UploadResult;
        var code = 0;
        var stdErr = "";
        if (error) {
            code = 2;
            stdErr = error.toString();
        } else {
            code = 0;
            var shCommand = main.commandPrefix + " " + main.binFilePath + " " + uploadObject.base;
            exec(shCommand, function (error, stdout, stderr) {
                // TODO get program result code and error textual representation and send
                // back in the response
                if (error) {
                    code = 1;
                    console.error(error);
                }

                var resultObject = new uploadResultClass({
                    "stdOut": stdout.toString(),
                    "resultCode": code,
                    "stdErr": stderr.toString()
                });
                var byteBuffer = resultObject.encode();
                var byteArray = [];
                for (var i = 0; i < byteBuffer.limit; i++) {
                    byteArray.push(byteBuffer.buffer[i]);
                }
                response.writeHead(200, {"Content-Type": "text/plain", "Content-length": byteArray.length});
                response.write(new Buffer(byteArray));
                response.end();
                console.log(shCommand);
            });
        }

    });
}

function other(httpContent, response) {
    console.log("other");
    response.writeHead(200, {"Content-Type": "text/plain"});
    response.end();
}


exports.loadBin = loadBin;
exports.other = other;