/**
 * Created by user on 7/21/16.
 */

const fs = require("fs");
const main = require("./../main.js");
const exec = require('child_process').exec;

function handle(httpContent, response) {
    var uploadClass = main.protoConstructorCarkot.Upload;
    var uploadObject = uploadClass.decode(httpContent);
    fs.writeFile(main.binFilePath, uploadObject.data.buffer, "binary", function (error) {
        var uploadResultClass = main.protoConstructorCarkot.UploadResult;
        var code = 0;
        var stdErr = "";
        if (error) {
            code = 2;
            stdErr = error.toString();
        } else {
            code = 0;
            var shCommand = main.commandPrefix + " " + main.binFilePath + " " + uploadObject.base;
            exec(shCommand, function (error, stdout, stderr) {
                if (error) {
                    code = 1;
                    console.error(error);
                }

                var resultObject = new uploadResultClass({
                    "stdOut": stdout.toString(),
                    "resultCode": code,
                    "stdErr": (stderr.toString() + "\n error:" + error)
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

exports.handler = handle;